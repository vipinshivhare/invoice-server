package in.vipinshivhare.invoicegeneratorapi.controller;

import in.vipinshivhare.invoicegeneratorapi.entity.User;
import in.vipinshivhare.invoicegeneratorapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(
    origins = {"https://invoicee-generator.netlify.app", "http://localhost:5173"},
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class UserController {

    private final UserService userService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public User createOrUpdateUser(@RequestBody User user, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not authenticated");
        }
        if (!authentication.getName().equals(user.getClerkId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User does not have permission to access this resource");
        }
        return userService.saveOrUpdateUser(user);
    }
}
