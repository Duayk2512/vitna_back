package com.vitna;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
class AuthRateLimit extends OncePerRequestFilter {
 record Window(long start,int count){}
 final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String path=request.getRequestURI();
  if("POST".equals(request.getMethod())&&java.util.Set.of("/api/login","/api/public/password/forgot","/api/public/password/reset").contains(path)){
   long now=System.currentTimeMillis();String key=request.getRemoteAddr()+":"+path;
   if(windows.size()>10000)windows.entrySet().removeIf(entry->now-entry.getValue().start()>900000);
   if(windows.size()>10000&&!windows.containsKey(key)){reject(response);return;}
   Window result=windows.compute(key,(ignored,old)->old==null||now-old.start()>900000?new Window(now,1):new Window(old.start(),old.count()+1));
   if(result.count()>30){reject(response);return;}
  }
  chain.doFilter(request,response);
 }
 void reject(HttpServletResponse response)throws IOException{response.setStatus(429);response.setHeader("Retry-After","900");response.setContentType("application/json");response.getWriter().write("{\"message\":\"Demasiados intentos. Espera 15 minutos antes de volver a intentar\"}");}
}
