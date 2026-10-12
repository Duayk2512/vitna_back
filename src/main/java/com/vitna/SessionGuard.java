package com.vitna;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
class SessionGuard extends OncePerRequestFilter {
 final Accounts accounts;
 SessionGuard(Accounts accounts){this.accounts=accounts;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null&&auth.getPrincipal() instanceof Security.SessionUser principal){
   var user=accounts.findByEmailIgnoreCase(principal.getUsername());
   if(user.isEmpty()||!user.get().active||user.get().credentialVersion!=principal.credentialVersion){
    var session=request.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();
    response.setStatus(401);response.setContentType("application/json");response.getWriter().write("{\"message\":\"La sesion cambio. Ingresa nuevamente\"}");return;
   }
  }
  chain.doFilter(request,response);
 }
}
