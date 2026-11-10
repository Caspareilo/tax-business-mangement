package com.example.tax.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Minimal Java EE 8 servlet used by the first course experiment.
 *
 * This class deliberately uses javax.servlet.* because the course project
 * targets Servlet 4.0 and Apache Tomcat 9.
 */
@WebServlet(name = "helloServlet", urlPatterns = "/hello")
public class HelloServlet extends HttpServlet {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void init() throws ServletException {
        log("HelloServlet init: instance created");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        log("HelloServlet service: " + request.getMethod() + " " + request.getRequestURI());
        super.service(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String name = request.getParameter("name");
        if (name == null || name.trim().isEmpty()) {
            name = "Java EE 8 学习者";
        }

        try (PrintWriter out = response.getWriter()) {
            out.println("<!doctype html>");
            out.println("<html lang=\"zh-CN\">");
            out.println("<head><meta charset=\"UTF-8\"><title>Servlet 生命周期验证</title>");
            out.println("<style>");
            out.println("body{font-family:'Microsoft YaHei',sans-serif;background:#f4f6f8;color:#1f2937;margin:0}");
            out.println("main{max-width:720px;margin:72px auto;background:#fff;border:1px solid #e5e7eb;border-radius:16px;padding:40px;box-shadow:0 12px 36px rgba(15,23,42,.08)}");
            out.println("h1{margin-top:0;font-size:28px}dl{display:grid;grid-template-columns:120px 1fr;gap:12px 20px}dt{color:#6b7280}dd{margin:0;font-weight:600}");
            out.println("code{background:#f3f4f6;padding:2px 6px;border-radius:4px}");
            out.println("</style></head>");
            out.println("<body><main>");
            out.println("<h1>Java EE 8 Servlet 运行成功</h1>");
            out.println("<dl>");
            out.println("<dt>访问参数 name</dt><dd>" + escapeHtml(name) + "</dd>");
            out.println("<dt>服务器时间</dt><dd>" + LocalDateTime.now().format(TIME_FORMAT) + "</dd>");
            out.println("<dt>Servlet 映射</dt><dd><code>/hello</code></dd>");
            out.println("<dt>响应编码</dt><dd><code>UTF-8</code></dd>");
            out.println("</dl>");
            out.println("<p>请求已经经过 EncodingFilter 和 HelloServlet，"
                    + "初始化、服务与销毁日志可在 Tomcat 日志中观察。</p>");
            out.println("</main></body></html>");
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    public void destroy() {
        log("HelloServlet destroy: instance removed");
    }
}
