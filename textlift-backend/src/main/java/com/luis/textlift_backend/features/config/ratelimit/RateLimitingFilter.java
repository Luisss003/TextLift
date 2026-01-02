package com.luis.textlift_backend.features.config.ratelimit;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final LoadingCache<String, Bucket> buckets;

    public RateLimitingFilter(LoadingCache<String, Bucket> buckets) {
        this.buckets = buckets;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException,IOException {
        // Use the direct remote address to avoid header spoofing
        String key = clientKey(request);
        Bucket bucket = buckets.get(key);
        if(bucket.tryConsume(1)){
            filterChain.doFilter(request, response);
        }
        else{
            response.setStatus(429);
            response.getWriter().write("{\"message\": \"Too many requests. Please try again later.\"}");
        }
    }

    private String clientKey(HttpServletRequest request) {
        // Avoid trusting X-Forwarded-For directly; rely on container/proxy config instead
        return request.getRemoteAddr();
    }


}
