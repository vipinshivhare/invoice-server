package in.vipinshivhare.invoicegeneratorapi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ClerkJwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ClerkJwtAuthFilter.class);

    @Value("${clerk.issuer}")
    private String clerkIssuer;

    private final ClerkJwksProvider jwksProvider;

    // Public endpoints (prefix-match supported)
    private final List<String> PUBLIC_PATHS = List.of(
            "/",
            "/health",
            "/actuator",
            "/api/webhooks",
            "/error" // <-- important for CORS & fallback
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ---------------------------------------------
        // 1️⃣ ALWAYS allow OPTIONS preflight
        // ---------------------------------------------
        if (HttpMethod.OPTIONS.matches(method)) {
            log.debug("CORS Preflight allowed: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // ---------------------------------------------
        // 2️⃣ Allow PUBLIC paths (prefix match)
        // ---------------------------------------------
        for (String pub : PUBLIC_PATHS) {
            if (path.equals(pub) || path.startsWith(pub + "/")) {
                log.debug("Public path allowed without auth: {}", path);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // ---------------------------------------------
        // 3️⃣ Protected routes must have Authorization header
        // ---------------------------------------------
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing/invalid Authorization header for {}", path);
            SecurityContextHolder.clearContext();

            // This must be 401, not 403
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        try {
            String token = authHeader.substring(7);

            // decode header → extract kid
            String[] parts = token.split("\\.");
            if (parts.length < 2) throw new IllegalArgumentException("Invalid JWT");

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode headerNode = mapper.readTree(headerJson);
            String kid = headerNode.has("kid") ? headerNode.get("kid").asText() : null;

            if (kid == null) throw new IllegalArgumentException("Missing kid");

            // fetch public key
            PublicKey publicKey = jwksProvider.getPublicKey(kid);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .setAllowedClockSkewSeconds(60)
                    .requireIssuer(clerkIssuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String clerkUserId = claims.getSubject();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            clerkUserId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authToken);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("JWT verification failed for {} {}: {}", method, path, e.getMessage());
            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Invalid JWT token");
        }
    }
}
