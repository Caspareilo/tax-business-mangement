package com.example.tax.action;

import com.example.tax.dto.Page;
import com.example.tax.exception.AppException;
import com.example.tax.model.TaxDeclaration;
import com.example.tax.security.SessionUser;
import com.example.tax.service.ReviewService;
import com.example.tax.util.WebUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class ReviewAction extends BaseAction {

    private static final long serialVersionUID = 1L;
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser user = currentUser(request);
        try {
            if ("/detail".equals(path)) {
                long id = WebUtil.longParam(request, "id", 0);
                request.setAttribute("declaration", reviewService.findForReview(user, id));
                request.setAttribute("pageTitle", "审核详情");
                forward(request, response, "reviews/detail.jsp");
            } else if (path.isEmpty() || "/pending".equals(path)) {
                Page<TaxDeclaration> pageData = reviewService.listPending(user,
                        WebUtil.pageParam(request), WebUtil.pageSizeParam(request));
                request.setAttribute("pageData", pageData);
                request.setAttribute("filterQuery", "");
                forward(request, response, "reviews/pending.jsp");
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (SQLException e) {
            renderServerError(request, response, e);
        } catch (AppException e) {
            handleAppException(request, response, e, "/reviews/pending.action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = pathInfo(request);
        SessionUser user = currentUser(request);
        long id = WebUtil.longParam(request, "id", 0);
        String comment = WebUtil.trim(request, "comment");
        try {
            if ("/approve".equals(path)) {
                reviewService.approve(user, id, comment, traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/reviews/pending.action",
                        "申报已审核通过");
            } else if ("/return".equals(path)) {
                reviewService.returnForRevision(user, id, comment, traceId(request));
                WebUtil.redirectWithSuccess(request, response, "/reviews/pending.action",
                        "申报已退回修改");
            } else {
                renderNotFound(request, response, "页面不存在");
            }
        } catch (AppException e) {
            String target = id > 0 ? "/reviews/detail.action?id=" + id : "/reviews/pending.action";
            handleAppException(request, response, e, target);
        } catch (SQLException e) {
            renderServerError(request, response, e);
        }
    }
}
