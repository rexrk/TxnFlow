package txnflow.gatewayservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class GatewayRequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String method = request.getMethod();
        String path = sanitizedPath(request.getRequestURI());
        String route = routeName(path);

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException ex) {
            log.error("Gateway request failed route={} method={} path={} error={}",
                    route,
                    method,
                    path,
                    ex.getClass().getSimpleName()
            );
            throw ex;
        } finally {
            int status = response.getStatus();
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;

            if (status >= 500) {
                log.error("Gateway route failed route={} method={} path={} status={} durationMs={}",
                        route,
                        method,
                        path,
                        status,
                        durationMs
                );
            } else if (status < 400 && isServiceRoute(path)) {
                log.info("Gateway route completed route={} method={} path={} status={} durationMs={}",
                        route,
                        method,
                        path,
                        status,
                        durationMs
                );
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null
                || path.equals("/error")
                || path.contains("/actuator");
    }

    private boolean isServiceRoute(String path) {
        return path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/wallets/");
    }

    private String routeName(String path) {
        if (path.startsWith("/api/v1/auth/")) {
            return "auth-service";
        }

        if (path.startsWith("/api/v1/wallets/")) {
            return "wallet-service";
        }

        return "unmatched";
    }

    private String sanitizedPath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        return path
                .replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{id}")
                .replaceAll("/\\d+", "/{id}");
    }
}
