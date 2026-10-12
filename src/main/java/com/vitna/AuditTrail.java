package com.vitna;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import jakarta.servlet.http.*;
import java.security.Principal;
@Component public class AuditTrail implements HandlerInterceptor,WebMvcConfigurer {
 final JdbcTemplate jdbc;
 public AuditTrail(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(this).addPathPatterns("/api/**");}
 @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){Principal actor=request.getUserPrincipal();if(actor!=null)request.setAttribute("vitna.audit.actor",actor.getName());return true;}
 @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception error){
  Object actor=request.getAttribute("vitna.audit.actor");
  if(actor==null||!java.util.Set.of("POST","PUT","PATCH","DELETE").contains(request.getMethod())||response.getStatus()<200||response.getStatus()>=300)return;
  try{jdbc.update("INSERT INTO audit_events(actor_id,actor_name,action,resource,status) SELECT id,name,?,?,? FROM accounts WHERE lower(email)=lower(?)",request.getMethod(),request.getRequestURI().substring(0,Math.min(300,request.getRequestURI().length())),response.getStatus(),actor.toString());}
  catch(Exception ex){org.slf4j.LoggerFactory.getLogger(AuditTrail.class).error("No se pudo persistir un evento de auditoria; revisar conexion de base");}
 }
}
