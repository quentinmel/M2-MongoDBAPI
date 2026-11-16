package fr.medislot.api;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdHeaderFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain filterChain)throws ServletException,IOException{
  if(request.getRequestURI().startsWith("/api/")){
   var context=Span.current().getSpanContext();
   if(context.isValid())response.setHeader("X-Trace-Id",context.getTraceId());
  }
  filterChain.doFilter(request,response);
 }
}
