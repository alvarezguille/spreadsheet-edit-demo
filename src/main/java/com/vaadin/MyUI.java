package com.vaadin;

import static com.vaadin.data.util.FilesystemContainer.PROPERTY_NAME;
import static com.vaadin.server.FontAwesome.FILE_EXCEL_O;
import static com.vaadin.server.Sizeable.Unit.PERCENTAGE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.annotation.WebServlet;

import com.vaadin.addon.spreadsheet.Spreadsheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.VerticalLayout;

/**
 *
 */
@Theme("mytheme")
@Widgetset("com.vaadin.MyAppWidgetset")
public class MyUI extends UI {

    private static final long serialVersionUID = -5813234435312189521L;

    private Spreadsheet spreadsheet;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalSplitPanel horizontalSplitPanel = new HorizontalSplitPanel();
        horizontalSplitPanel.setSplitPosition(300, Unit.PIXELS);
        setContent(horizontalSplitPanel);

        initSpreadsheet();

        horizontalSplitPanel.setFirstComponent(createToolbar());
        horizontalSplitPanel.setSecondComponent(spreadsheet);
    }

    private Component createToolbar() {
        final File templatesDirectory = new File("sheets" + File.separator);
        if (!templatesDirectory.exists()) {
            templatesDirectory.mkdir();
        }
        VerticalLayout toolbar = new VerticalLayout();
        toolbar.setSizeFull();
        toolbar.setSpacing(true);

        FilesystemContainer filesContainer = new FilesystemContainer(
                templatesDirectory);
        filesContainer.setRecursive(false);
        filesContainer.setFilter(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name != null
                        && (name.endsWith(".xls") || name.endsWith(".xlsx") || name
                                .endsWith(".xlsm"))) {
                    return true;
                } else {
                    File toDelete = new File(dir, name);
                    toDelete.delete();
                    return false;
                }
            }
        });

        final ListSelect availableFiles = new ListSelect("Available templates",
                filesContainer);
        availableFiles.setSizeFull();
        availableFiles.setIcon(FILE_EXCEL_O);
        availableFiles.setImmediate(true);
        availableFiles.setNullSelectionAllowed(false);
        availableFiles.setItemCaptionPropertyId(PROPERTY_NAME);
        availableFiles.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 8048738040109480947L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                Object value = availableFiles.getValue();
                if (value != null && value instanceof File) {
                    loadFile((File) value);
                }
            }
        });

        final Upload upload = new Upload();
        upload.setWidth(100, PERCENTAGE);
        upload.setButtonCaption("Add new template");
        upload.setImmediate(true);
        upload.setReceiver(new Receiver() {
            private static final long serialVersionUID = -2293616343861518740L;

            @Override
            public OutputStream receiveUpload(String filename, String mimeType) {
                try {
                    File tempFile = new File(templatesDirectory, filename);
                    return new FileOutputStream(tempFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        upload.addFinishedListener(new FinishedListener() {
            private static final long serialVersionUID = -8840790897095932946L;

            @Override
            public void uploadFinished(FinishedEvent event) {
                availableFiles.markAsDirty();
            }
        });
        final Button download = new Button("Download edited");
        download.setWidth(100, PERCENTAGE);
        download.setIcon(FontAwesome.DOWNLOAD);
        FileDownloader fileDownloader = new FileDownloader(new StreamResource(
                new StreamSource() {
                    private static final long serialVersionUID = 2076495798023012455L;

                    @Override
                    public InputStream getStream() {
                        if (spreadsheet != null) {
                            try {
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                spreadsheet.write(bos);
                                return new ByteArrayInputStream(bos
                                        .toByteArray());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }
                }, "export.xlsx"));
        fileDownloader.extend(download);

        toolbar.addComponent(upload);
        toolbar.addComponent(availableFiles);
        toolbar.addComponent(download);
        toolbar.setExpandRatio(availableFiles, 1);

        return toolbar;
    }

    private void loadFile(File file) {
        try {
            spreadsheet.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initSpreadsheet() {
        spreadsheet = new Spreadsheet();
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
