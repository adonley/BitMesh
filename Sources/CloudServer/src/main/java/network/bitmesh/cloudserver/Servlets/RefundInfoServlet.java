package network.bitmesh.cloudserver.Servlets;

import network.bitmesh.cloudserver.Statistics.RefundPost;
import network.bitmesh.cloudserver.Utils.PersistenceHelper;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class RefundInfoServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        List<? extends RefundPost> transactions = PersistenceHelper.getRefundTxs();
        req.setAttribute("transactions", transactions);
        RequestDispatcher rd = req.getRequestDispatcher("posted.jsp");
        rd.forward(req, resp);
    }
}
