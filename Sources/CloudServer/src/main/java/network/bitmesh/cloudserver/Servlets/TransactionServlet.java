package network.bitmesh.cloudserver.Servlets;

import network.bitmesh.cloudserver.Bitcoin.LockedTransactionBroadcasterTimer;
import network.bitmesh.cloudserver.Statistics.RefundPost;
import network.bitmesh.cloudserver.Utils.CommonResponses;
import network.bitmesh.cloudserver.ServerConfig;
import network.bitmesh.cloudserver.Utils.PersistenceHelper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;

public class TransactionServlet extends HttpServlet
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TransactionServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // Allow cross origin requests
        CommonResponses.allowCrossOrigin(resp);

        // Really large transaction, 5BTC miner fee?
        byte[] transaction = new byte[req.getContentLength()];

        InputStream transactionInputStream = req.getInputStream();
        PrintWriter out = resp.getWriter();

        long startTime = System.nanoTime();

        int alreadyRead = 0;
        while(req.getContentLength() > alreadyRead && !CommonResponses.shouldTimeOut(startTime))
        {
            alreadyRead += transactionInputStream.read(transaction, alreadyRead, transactionInputStream.available());
        }

        Transaction trans = null;
        boolean isMainNet = true;

        try
        {
            log.info("Received transaction post - TX: " + trans.getHashAsString());
            trans = new Transaction(MainNetParams.get(), transaction);
            trans.verify();
        }
        catch(Exception e)
        {
            try
            {
                isMainNet = false;
                log.info("Parsing as TestNet");
                trans = new Transaction(TestNet3Params.get(), transaction);
                trans.verify();
            }
            catch (Exception e1)
            {
                log.error("Error parsing transaction.");
                resp.setStatus(resp.SC_BAD_REQUEST);

                if(ServerConfig.BITMESH_TEST)
                    out.println("<html><body><p>Failed to parse transaction: " + ExceptionUtils.getStackTrace(e) + "</p></body></html>");
                else
                    out.println("<html><body><p>Failed to parse transaction.</p></body></html>");

                out.close();
                e.printStackTrace();

                return;
            }
        }


        LockedTransactionBroadcasterTimer.getInstance().scheduleTransactionBroadcast(trans);

        resp.setStatus(resp.SC_OK);
        out.println("Transaction will be posted at: " + new Date(trans.getLockTime()*1000).toString());
        out.close();

        // Record statistics about the refund transaction
        RefundPost refundPost = new RefundPost(trans, getIP(req), isMainNet);
        PersistenceHelper.addRefund(refundPost);
        return;
    }

    /**
     * jQuery asks for Options before it does the ajax request. We have to add
     * the CORS headers to this request.
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        CommonResponses.allowCrossOrigin(resp);
        resp.setStatus(resp.SC_OK);
    }

    /**
     * Gets the IP from the request, trying the x-forwarded-for first, then remote address
     * @param req - the request the host sent out
     * @return - the IP that the remote host is on
     */
    protected String getIP(HttpServletRequest req)
    {
        String ipAddress = req.getHeader("x-forwarded-for");
        if (ipAddress == null)
            ipAddress = req.getRemoteAddr();
        return ipAddress;
    }

}
