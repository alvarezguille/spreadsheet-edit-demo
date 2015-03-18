package com.vaadin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.annotation.WebServlet;

import com.vaadin.addon.spreadsheet.Spreadsheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
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

    private File tempFile;
    private Spreadsheet spreadsheet;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setSizeFull();
        setContent(layout);

        initSpreadsheet();

        layout.addComponent(createToolbar());
        layout.addComponent(spreadsheet);
        layout.setExpandRatio(spreadsheet, 1);
    }

    private Component createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setSpacing(true);

        final Upload upload = new Upload();
        upload.setReceiver(new Receiver() {

            @Override
            public OutputStream receiveUpload(String filename, String mimeType) {
                try {
                    tempFile = new File(filename);
                    tempFile.deleteOnExit();

                    FileOutputStream fos = new FileOutputStream(tempFile);
                    return fos;
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                return null;
            }
        });
        upload.addFinishedListener(new FinishedListener() {

            @Override
            public void uploadFinished(FinishedEvent event) {
                if (tempFile != null) {
                    try {
                        spreadsheet.read(tempFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        tempFile.delete();
                    }
                }
            }
        });
        upload.setImmediate(true);
        Button download = new Button("Download");
        FileDownloader fileDownloader = new FileDownloader(new StreamResource(
                new StreamSource() {

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
        toolbar.addComponent(download);
        return toolbar;
    }

    private void initSpreadsheet() {
        spreadsheet = new Spreadsheet();
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
