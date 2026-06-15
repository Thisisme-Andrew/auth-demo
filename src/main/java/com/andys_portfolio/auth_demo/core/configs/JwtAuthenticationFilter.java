package com.andys_portfolio.auth_demo.core.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.andys_portfolio.auth_demo.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");
		final String jwt; // the actual JWT Object that contains header, claims and signature
		final String userEmail;

		// This is not to do with auth can just go with rest of filterchain
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		// Extract JWT token (remove "Bearer " prefix)
		jwt = authHeader.substring(7);

		try {
			userEmail = jwtService.extractUsername(jwt);
		} catch (JwtException | IllegalArgumentException e) {
			log.debug("Invalid or expired JWT: {} - {}", e.getClass().getSimpleName(), e.getMessage());
			filterChain.doFilter(request, response);
			return;
		}

		// Check if email exists and user is not already authenticated
		if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

			UserDetails userDetails;
			try {
				userDetails = this.userDetailsService.loadUserByUsername(userEmail);
			} catch (UsernameNotFoundException e) {
				log.warn("JWT valid but user not found: {}", userEmail);
				filterChain.doFilter(request, response);
				return;
			}

			// Validate token
			if (jwtService.isTokenValid(jwt, userDetails)) {
				// Create authentication token
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					userDetails,
					null,
					userDetails.getAuthorities()
				);

				// Set authentication details
				authToken.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(request)
				);

				// Set authentication in security context
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}

		// Continue filter chain
		filterChain.doFilter(request, response);
	}
}