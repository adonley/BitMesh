package network.bitmesh.cloudserver.Servlets;

import com.google.gson.Gson;
import info.blockchain.api.APIException;
import info.blockchain.api.blockexplorer.*;
import network.bitmesh.cloudserver.Utils.CommonResponses;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;

public class UnspentTransactionServlet extends HttpServlet
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(UnspentTransactionServlet.class.getName());

    /**
     * Shit only works for main-net right now.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        CommonResponses.allowCrossOrigin(resp);

        InputStreamReader inputStreamReader =  new InputStreamReader(req.getInputStream(), "utf-8");
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        int b;
        StringBuilder addressString = new StringBuilder(512);

        while ((b = bufferedReader.read()) != -1)
        {
            addressString.append((char) b);
        }

        bufferedReader.close();
        inputStreamReader.close();

        log.info("Getting unspent outputs for: " + addressString.toString());

        try
        {
            BlockExplorer blockExplorer = new BlockExplorer();
            List<UnspentOutput> unspentOutputs = blockExplorer.getUnspentOutputs(addressString.toString());

            for(UnspentOutput unspent : unspentOutputs)
            {
                log.info("Unspent: " + unspent.toString());
            }

            Gson gson = new Gson();
            String json = gson.toJson(unspentOutputs);
            Writer writer = resp.getWriter();
            writer.write(json);
        }
        catch (APIException epi)
        {
            log.error("Error with address posted.");
            epi.printStackTrace();
            resp.setStatus(405);
            return;
        }

        resp.setStatus(200);
    }
}
