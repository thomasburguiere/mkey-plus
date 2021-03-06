package fr.lis.mkeyplusWSREST.service;

import com.google.common.io.Closeables;
import fr.lis.mkeyplusWSREST.constant.Constant;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;

// Plain old Java Object it does not extend as class or implements 
// an interface

// The class registers its methods for the HTTP GET request using the @GET annotation. 
// Using the @Produces annotation, it defines that it can deliver several MIME types,
// text, XML and HTML. 

// The browser requests per default the HTML MIME type.

//Sets the path to base URL + /hello
@Path("/history")
public class HistoryWebService {

    public static final int INITIAL_SIZE = 2048;

    // This method is called if TEXT_PLAIN is request
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayPlainTextHello() {
        return "Available services : 'send' and 'get'";
    }

    @SuppressWarnings("SizeReplaceableByIsEmpty")
    @Path("/send")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String sendDescription(@QueryParam("callback") String callback,
                                  @QueryParam("sddversion") String sddVersion,
                                  @QueryParam("spipollsessionid") String spipollsessionid,
                                  @QueryParam("history") String jsonDescriptions,
                                  @QueryParam("urlimageuser") String urlImageUser,
                                  @QueryParam("itemsselected") String jsonItemsSelected) {
        String jsonData;
        String status = "ok";
        String message = "";

        if (jsonItemsSelected == null || jsonDescriptions == null) {
            return "{ 'status' : 'Error' , 'message' : 'Error missing parameters' , 'spipollsessionid' : '"
                    + null + "' }";
        }

        // If no spipollsessionid, we generate one with a random number
        if (spipollsessionid == null) {
            spipollsessionid = Math.round(Math.random()
                    * Constant.MAX_RAND_NUMBER_UNKNOWN)
                    + "unknow";
        }
        if (urlImageUser == null) {
            urlImageUser = "none";
        }

        if (sddVersion == null) {
            sddVersion = "unknown";
        }

        // Image Downloading -------------------------
        URL url;
        if (!"none".equals(urlImageUser)) {
            OutputStream os = null;
            try {
                url = new URL(urlImageUser);

                String fileName = url.getFile();

                String destName = Constant.UPLOAD_DESTINATION + spipollsessionid
                        + fileName.substring(fileName.lastIndexOf("."));

                InputStream is = url.openStream();
                os = new FileOutputStream(destName);

                byte[] b = new byte[INITIAL_SIZE];
                int length;

                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }

                is.close();
                os.close();
                System.out.println(destName + " => copied !");
                System.out.println(urlImageUser + " image !");
            } catch (IOException e) {
                e.printStackTrace();
                status = "Erreur";
                message = "Error during the download of : " + urlImageUser;
            } finally {
                if (os != null) {
                    try {
                        Closeables.close(os, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // end Image Downloading -------------------------

        // Creating File containing historic & RemainingSelected
        PrintWriter writer;
        String filetxtname = Constant.UPLOAD_DESTINATION + spipollsessionid
                + "." + Constant.EXTENSION_TEXTFILE;
        try {

            writer = new PrintWriter(filetxtname, Constant.ENCODING_TEXTFILE);
            writer.println("{ '" + Constant.ENTRY_NAME_SDDVERSION + "' : '"
                    + sddVersion + "', '" + Constant.ENTRY_NAME_DESCRIPTIONS
                    + "' : " + jsonDescriptions + "  , '"
                    + Constant.ENTRY_NAME_TAXA + "' : " + jsonItemsSelected
                    + " }");
            writer.close();
            System.out.println(filetxtname + " => created !");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            status = "Error";
            message = "Error during the creation of : " + filetxtname;
        } catch (UnsupportedEncodingException e) {
            status = "Error";
            message = "Error of encoding during the creation of : "
                    + filetxtname;
        }

        // end Creating File -------------------------

        jsonData = "{ 'status' : '" + status + "' , 'message' : '" + message
                + "' , 'spipollsessionid' : '" + spipollsessionid + "' }";
        if (callback != null && !callback.trim().isEmpty()) {
            return callback + "(" + jsonData + ")";
        }

        return jsonData;
    }


    @SuppressWarnings("SizeReplaceableByIsEmpty")
    @Path("/get")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    public String getDescription(@QueryParam("callback") String callback,
                                 @QueryParam("spipollsessionid") String spipollsessionid) {
        String jsonData;
        String status = "ok";
        StringBuilder value = new StringBuilder();
        String image_url = "none";

        if (spipollsessionid == null) {
            return "{ 'status' : 'Error' , 'message' : 'Error missing parameter : spipollsessionid'  }";
        }

        // Get data -------------------
        String data_filename = Constant.UPLOAD_DESTINATION + spipollsessionid
                + "." + Constant.EXTENSION_TEXTFILE;
        data_filename = data_filename.replace("\\", "\\\\");
        try {
            InputStream ips = new FileInputStream(data_filename);
            InputStreamReader ipsr = new InputStreamReader(ips, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(ipsr);
            String ligne;
            while ((ligne = br.readLine()) != null) {
                value.append(ligne);
            }
            br.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        // end Get data -------------------

        // Get URL Image -------------------
        File folder = new File(Constant.UPLOAD_DESTINATION);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                String[] filename = file.getName().split("\\.");
                if (!filename[1].equalsIgnoreCase(Constant.EXTENSION_TEXTFILE)
                        && spipollsessionid.equals(filename[0])) {
                    image_url = Constant.URL_HISTOCK + file.getName();
                    break;
                }
            }
        }
        // end Get URL Image -------------------

        // end Creating File -------------------------
        jsonData = "{ 'status' : '" + status + "' , 'data' : " + value.toString()
                + " ,  'urlimage' : '" + image_url + "' , }";
        if (callback != null && !callback.trim().isEmpty()) {
            return callback + "(" + jsonData + ")";
        }

        return jsonData;
    }

}