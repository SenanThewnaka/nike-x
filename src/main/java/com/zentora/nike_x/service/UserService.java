package com.zentora.nike_x.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.entity.*;
import com.zentora.nike_x.mail.ResetPasswordMail;
import com.zentora.nike_x.mail.VerificationMail;
import com.zentora.nike_x.provider.MailServiceProvider;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import com.zentora.nike_x.util.SecurityUtil;
import com.zentora.nike_x.validation.Validator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class UserService {

    public String addNewUser(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getFirstName() == null) {
            message = "First name is required!";
        } else if (userDTO.getFirstName().isBlank()) {
            message = "First name can not be empty!";
        } else if (userDTO.getLastName() == null) {
            message = "Last name is required!";
        } else if (userDTO.getLastName().isBlank()) {
            message = "Last name can not be empty!";
        } else if (userDTO.getEmail() == null) {
            message = "Email is required!";
        } else if (userDTO.getEmail().isBlank()) {
            message = "Email can not be empty!";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email address!";
        } else if (userDTO.getMobile() == null) {
            message = "Mobile number is required!";
        } else if (userDTO.getMobile().isBlank()) {
            message = "Mobile number can not be empty!";
        } else if (!userDTO.getMobile().matches(Validator.MOBILE_VALIDATION)) {
            message = "Please provide valid mobile number!";
        } else if (userDTO.getGenderId() == null || !userDTO.getGenderId().equals(1) || userDTO.getGenderId().equals(2)
                || userDTO.getGenderId().equals(3)) {
            message = "Please select a valid gender!";
        } else if (userDTO.getPassword() == null) {
            message = "Password is required!";
        } else if (userDTO.getPassword().isBlank()) {
            message = "Password can not be empty!";
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Please provide valid password. \n " +
                    "The password must  8 to 25 characters long and include at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character";
        } else {

            String hashedPassword = SecurityUtil.hashPassword(userDTO.getPassword());
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                    .setParameter("email", userDTO.getEmail())
                    .getSingleResultOrNull();

            if (singleUser != null) { // Already exists
                message = "This email already exists! Please use another email";
            } else {
                User u = new User();
                u.setFirstName(userDTO.getFirstName());
                u.setLastName(userDTO.getLastName());
                u.setEmail(userDTO.getEmail());
                u.setPassword(hashedPassword);
                Gender gender = hibernateSession.createQuery("FROM Gender g WHERE g.id=:id", Gender.class)
                        .setParameter("id", userDTO.getGenderId()).getSingleResult();
                u.setGender(gender);

                String verificationCode = AppUtil.generateCode();

                u.setVerificationCode(verificationCode);

                Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("type", String.valueOf(Status.Type.PENDING)).getSingleResult();

                u.setStatus(pendingStatus);

                Mobile mobile = new Mobile();
                mobile.setMobile(userDTO.getMobile());
                mobile.setUser(u);
                Transaction transaction = hibernateSession.beginTransaction();

                try {
                    hibernateSession.persist(u);
                    hibernateSession.persist(mobile);
                    transaction.commit();

                    /// verification-mail-sending-start
                    VerificationMail verificationMail = new VerificationMail(u.getEmail(), verificationCode);
                    MailServiceProvider.getMailServiceProvider().sendMail(verificationMail);
                    /// verification-mail-sending-end

                    status = true;
                    message = "Account created successfully. Verification code has been sent to the your email. " +
                            "Please verify it for activate your account!";

                } catch (HibernateException e) {
                    transaction.rollback();
                    message = "Account creation failed. Please try again!";
                }

            }
            hibernateSession.close();
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateEmail(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            message = "Current email is required!";
        } else if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            message = "Password is required!";
        } else if (userDTO.getNewEmail() == null || userDTO.getNewEmail().isBlank()) {
            message = "New email is required!";
        } else if (userDTO.getNewEmail().equals(userDTO.getEmail())) {
            message = "New email can not be same as current email!";
        } else {

            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();

            try {

                User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", userDTO.getEmail())
                        .getSingleResultOrNull();

                User newUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", userDTO.getNewEmail()).getSingleResultOrNull();
                if (singleUser == null) {
                    message = "Please Sign up first!";
                } else if (newUser != null) {
                    message = "An account already exists with the new email address provided!.";
                } else {

                    boolean isPasswordMatched = SecurityUtil.checkPassword(userDTO.getPassword(),
                            singleUser.getPassword());

                    if (!isPasswordMatched) {
                        message = "Invalid email or password!";
                    } else {

                        singleUser.setEmail(userDTO.getNewEmail());
                        String verificationCode = AppUtil.generateCode();
                        singleUser.setVerificationCode(verificationCode);

                        Transaction transaction = hibernateSession.beginTransaction();
                        try {

                            hibernateSession.update(singleUser);
                            transaction.commit();

                            VerificationMail verificationMail = new VerificationMail(singleUser.getEmail(),
                                    verificationCode);
                            MailServiceProvider.getMailServiceProvider().sendMail(verificationMail);

                            status = true;
                            message = "Email updated successfully! Please check your email for verification code.";
                        } catch (HibernateException e) {
                            transaction.rollback();
                            message = "Email update failed. Please try again!.";
                        }
                    }
                }
            } catch (Exception e) {
                message = "An error occurred.";
            }
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String resendCode(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                .setParameter("email", userDTO.getEmail())
                .getSingleResultOrNull();

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            message = "something went wrong!, Plese try again!";
        } else if (singleUser == null) {
            message = "Please Sign up first!";
        } else {
            String verificationCode = AppUtil.generateCode();
            singleUser.setVerificationCode(verificationCode);
            Transaction transaction = hibernateSession.beginTransaction();
            try {
                hibernateSession.merge(singleUser);
                transaction.commit();

                VerificationMail verificationMail = new VerificationMail(singleUser.getEmail(), verificationCode);
                MailServiceProvider.getMailServiceProvider().sendMail(verificationMail);

                status = true;
                message = "Verification code has been sent to your email. Please verify it for activate your account!";
            } catch (HibernateException e) {
                transaction.rollback();
                message = "something went wrong!, Please try again!";
                System.out.println("something went wrong!" + e.getMessage());
            }
        }

        hibernateSession.close();

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String verifyCode(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            responseObject.addProperty("status", false);
            responseObject.addProperty("message", "Email is required!");
            return AppUtil.GSON.toJson(responseObject);
        }

        try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

            User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                    .setParameter("email", userDTO.getEmail())
                    .getSingleResultOrNull();

            if (singleUser == null) {
                message = "Please sign up first!";
            } else if (userDTO.getVerificationCode() == null || userDTO.getVerificationCode().isBlank()) {
                message = "Verification code is required!";
            } else if (!userDTO.getVerificationCode().matches(Validator.VERIFICATION_CODE_VALIDATION)) {
                message = "Invalid verification code!";
            } else if (!userDTO.getVerificationCode().equals(singleUser.getVerificationCode())) {
                message = "Incorrect verification code!";
            } else {

                Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("type", String.valueOf(Status.Type.VERIFIED))
                        .getSingleResult();

                if (singleUser.getStatus().equals(verifiedStatus)) {
                    message = "Account already verified!";
                } else {

                    Transaction transaction = hibernateSession.beginTransaction();
                    try {
                        singleUser.setStatus(verifiedStatus);
                        singleUser.setVerificationCode(null);
                        hibernateSession.merge(singleUser);

                        transaction.commit();
                        status = true;
                        message = "Account verification completed!";
                    } catch (Exception e) {
                        transaction.rollback();
                        message = "Something went wrong. Verification failed!";
                    }
                }
            }

            responseObject.addProperty("status", status);
            responseObject.addProperty("message", message);
            return AppUtil.GSON.toJson(responseObject);

        }
    }

    public String forgotPasswordCode(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            message = "Please enter your email address.";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide a valid email address!";
        } else {

            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

                User user = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", userDTO.getEmail())
                        .getSingleResultOrNull();

                Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("type", String.valueOf(Status.Type.VERIFIED))
                        .getSingleResult();
                Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("type", String.valueOf(Status.Type.PENDING))
                        .getSingleResult();
                if (user == null) {
                    message = "Please sign up first!";
                } else if (!user.getStatus().equals(verifiedStatus) && !user.getStatus().equals(pendingStatus)) {
                    message = "You cannot reset your password. Please contact admin.";
                } else {

                    String verificationCode = AppUtil.generateCode();
                    user.setVerificationCode(verificationCode);

                    Transaction transaction = hibernateSession.beginTransaction();

                    try {
                        hibernateSession.merge(user);
                        transaction.commit();

                        ResetPasswordMail resetPasswordMail = new ResetPasswordMail(user.getEmail(), verificationCode);

                        MailServiceProvider.getMailServiceProvider().sendMail(resetPasswordMail);

                        status = true;
                        message = "A code to reset your password has been sent to your email.";
                    } catch (HibernateException e) {
                        if (transaction != null)
                            transaction.rollback();
                        message = "Something went wrong. Please try again.";
                        System.out.println("Hibernate Error: " + e.getMessage());
                    }
                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String ResetPassword(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            message = "Something went wrong!, Please try again!";
        } else {
            if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
                message = "Something went wrong!, Please try again!";
            } else if (userDTO.getVerificationCode() == null || userDTO.getVerificationCode().isBlank()) {
                message = "Please enter the reset password code.";
            } else if (!userDTO.getVerificationCode().matches(Validator.VERIFICATION_CODE_VALIDATION)) {
                message = "Invalid verification code!";
            } else if (userDTO.getNewPassword() == null || userDTO.getNewPassword().isBlank()) {
                message = "Please enter a new password.";
            } else if (!userDTO.getNewPassword().matches(Validator.PASSWORD_VALIDATION)) {
                message = "Please provide valid password. \\n \" +\n" +
                        "                    \"The password must  8 to 25 characters long and include at least one uppercase letter, \" +\n"
                        +
                        "                    \"one lowercase letter, one digit, and one special character";
            } else if (userDTO.getConfirmPassword() == null || userDTO.getConfirmPassword().isBlank()) {
                message = "Please confirm your new password.";
            } else if (!userDTO.getNewPassword().equals(userDTO.getConfirmPassword())) {
                message = "New password and confirm password do not match.";
            } else {

                try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {

                    User user = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                            .setParameter("email", userDTO.getEmail())
                            .getSingleResultOrNull();

                    Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("type", String.valueOf(Status.Type.VERIFIED))
                            .getSingleResult();
                    Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("type", String.valueOf(Status.Type.PENDING))
                            .getSingleResult();

                    String hashedPassword = SecurityUtil.hashPassword(userDTO.getNewPassword());
                    if (user == null) {
                        message = "Please sign up first!";
                    } else if (!user.getStatus().equals(verifiedStatus) && !user.getStatus().equals(pendingStatus)) {
                        message = "You cannot reset your password. Please contact admin.";
                    } else if (!userDTO.getVerificationCode().equals(user.getVerificationCode())) {
                        message = "Invalid reset code!";
                    } else {

                        Transaction transaction = hibernateSession.beginTransaction();

                        user.setPassword(hashedPassword);
                        user.setVerificationCode(null);
                        try {
                            hibernateSession.merge(user);
                            transaction.commit();

                            status = true;
                            message = "Your password has been reset successfully.Please login with your new password.";
                        } catch (HibernateException e) {
                            if (transaction != null)
                                transaction.rollback();
                            message = "Something went wrong. Please try again.";
                            System.out.println("Hibernate Error: " + e.getMessage());
                        }
                    }
                }
            }

        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);

    }

    public String login(UserDTO userDTO, HttpServletRequest request, HttpServletResponse response) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            message = "Email is required!";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email address!";
        } else if (userDTO.getPassword() == null || userDTO.getPassword().isBlank()) {
            message = "Password is required!";
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Please provide valid password. \n " +
                    "The password must   8 to 25 characters long and include at least one uppercase letter, " +
                    "one lowercase letter, one digit, and one special character";
        } else {
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                User user = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                        .setParameter("email", userDTO.getEmail())
                        .getSingleResultOrNull();

                Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("type", String.valueOf(Status.Type.VERIFIED))
                        .getSingleResult();
                if (user == null) {
                    message = "Invalid email or password!";
                } else {
                    if (SecurityUtil.checkPassword(userDTO.getPassword(), user.getPassword())) {
                        if (user.getStatus().equals(verifiedStatus)) {
                            status = true;
                            message = "Login successful!";

                            // Store UserDTO in session instead of Entity
                            UserDTO sessionUser = new UserDTO();
                            sessionUser.setId(user.getId());
                            sessionUser.setEmail(user.getEmail());
                            sessionUser.setFirstName(user.getFirstName());
                            sessionUser.setLastName(user.getLastName()); // Assuming standard user

                            request.getSession().setAttribute("user", sessionUser);

                            // Merge Session Cart to DB Cart
                            new CartService().mergeCarts(user.getId(), request.getSession());
                            request.getSession().removeAttribute("cart");

                            if (userDTO.isRememberMe()) {
                                Cookie cookie = new Cookie("email", userDTO.getEmail());
                                cookie.setMaxAge(60 * 60 * 24 * 7); // 7 Days
                                cookie.setPath("/");
                                response.addCookie(cookie);

                                Cookie passwordCookie = new Cookie("password", userDTO.getPassword());
                                passwordCookie.setMaxAge(60 * 60 * 24 * 7); // 7 Days
                                passwordCookie.setPath("/");
                                response.addCookie(passwordCookie);
                            } else {
                                Cookie[] cookies = request.getCookies();
                                if (cookies != null) {
                                    for (Cookie cookie : cookies) {
                                        cookie.setMaxAge(0);
                                        cookie.setPath("/");
                                        response.addCookie(cookie);
                                    }
                                }
                            }

                        } else {
                            message = "Please verify your email first!";
                        }
                    } else {
                        message = "Invalid email or password!";
                    }
                }
            } catch (HibernateException e) {
                message = "Something went wrong. Please try again!";
                System.out.println("Hibernate Error: " + e.getMessage());
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String checkRememberMe(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String email = "";
        String password = "";

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("email")) {
                    email = cookie.getValue();
                } else if (cookie.getName().equals("password")) {
                    password = cookie.getValue();
                }
            }
        }

        if (!email.isEmpty() && !password.isEmpty()) {
            status = true;
            responseObject.addProperty("email", email);
            responseObject.addProperty("password", password);
        }

        responseObject.addProperty("status", status);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String checkAuth(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;

        // Expect UserDTO now
        UserDTO user = (UserDTO) request.getSession().getAttribute("user");

        if (user != null) {
            status = true;
            JsonObject userObj = new JsonObject();
            userObj.addProperty("firstName", user.getFirstName());
            userObj.addProperty("lastName", user.getLastName());
            userObj.addProperty("email", user.getEmail());
            responseObject.add("user", userObj);
        }

        responseObject.addProperty("status", status);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String setPrimaryAddress(int addressId, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (request.getSession(false) == null || request.getSession().getAttribute("user") == null) {
            message = "Please sign in first!";
        } else {
            UserDTO sessionUser = (UserDTO) request.getSession().getAttribute("user");

            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = hibernateSession.beginTransaction();
                try {
                    User user = hibernateSession.get(User.class, sessionUser.getId());

                    // Verify target address
                    Address targetAddress = hibernateSession.get(Address.class, addressId);
                    if (targetAddress == null) {
                        throw new Exception("Address not found");
                    }
                    if (!targetAddress.getUser().getId().equals(user.getId())) {
                        throw new Exception("Unauthorized");
                    }

                    // Reset all user addresses to 0
                    for (Address addr : user.getAddresses()) {
                        addr.setIsPrimary((byte) 0);
                        hibernateSession.merge(addr);
                    }

                    // Set target to 1
                    targetAddress.setIsPrimary((byte) 1);
                    hibernateSession.merge(targetAddress);

                    transaction.commit();
                    status = true;
                    message = "Primary address updated!";
                } catch (Exception e) {
                    if (transaction != null)
                        transaction.rollback();
                    e.printStackTrace();
                    message = "Error updating primary address";
                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String getUserProfile(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "Please sign in first!";

        if (request.getSession(false) != null && request.getSession().getAttribute("user") != null) {
            UserDTO sessionUser = (UserDTO) request.getSession().getAttribute("user");

            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                User user = hibernateSession.get(User.class, sessionUser.getId());

                if (user != null) {
                    status = true;
                    message = "Success";

                    JsonObject userObj = new JsonObject();
                    // ... existing fields ...
                    userObj.addProperty("firstName", user.getFirstName());
                    userObj.addProperty("lastName", user.getLastName());
                    userObj.addProperty("email", user.getEmail());

                    // Format Date
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yyyy");
                    userObj.addProperty("registeredDate", sdf.format(user.getCreatedAt()));

                    // Mobiles
                    com.google.gson.JsonArray mobileArray = new com.google.gson.JsonArray();
                    for (Mobile m : user.getMobiles()) {
                        if (Boolean.TRUE.equals(m.getDeleted()))
                            continue; // Skip deleted
                        JsonObject mObj = new JsonObject();
                        mObj.addProperty("id", m.getId());
                        mObj.addProperty("mobile", m.getMobile());
                        mobileArray.add(mObj);
                    }
                    userObj.add("mobiles", mobileArray);

                    // Addresses
                    JsonArray addressArray = new JsonArray();
                    java.util.Set<Integer> addedIds = new java.util.HashSet<>();

                    // Sort addresses: Primary first (1 > 0), then by ID desc
                    java.util.List<Address> sortedAddresses = new java.util.ArrayList<>(user.getAddresses());
                    sortedAddresses.sort((a1, a2) -> {
                        int p1 = (a1.getIsPrimary() != null) ? a1.getIsPrimary() : 0;
                        int p2 = (a2.getIsPrimary() != null) ? a2.getIsPrimary() : 0;
                        if (p1 != p2)
                            return p2 - p1; // Descending primary status
                        return a2.getId() - a1.getId(); // Descending ID
                    });

                    for (Address a : sortedAddresses) {
                        if (addedIds.contains(a.getId()))
                            continue;
                        if (Boolean.TRUE.equals(a.getDeleted()))
                            continue; // Skip deleted

                        addedIds.add(a.getId());

                        JsonObject aObj = new JsonObject();
                        aObj.addProperty("id", a.getId());
                        aObj.addProperty("lineOne", a.getLineOne());
                        aObj.addProperty("lineTwo", a.getLineTwo());
                        aObj.addProperty("postalCode", a.getPostalCode());
                        aObj.addProperty("cityId", a.getCity().getId());
                        aObj.addProperty("city", a.getCity().getName());
                        aObj.addProperty("isPrimary", (a.getIsPrimary() != null && a.getIsPrimary() == 1));
                        addressArray.add(aObj);
                    }
                    userObj.add("addresses", addressArray);

                    responseObject.add("user", userObj);
                }
            } catch (HibernateException e) {
                System.out.println("Hibernate Error: " + e.getMessage());
                message = "Error fetching profile";
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String saveAddress(UserDTO userDTO, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (request.getSession(false) == null || request.getSession().getAttribute("user") == null) {
            message = "Please sign in first!";
        } else if (userDTO.getLineOne() == null || userDTO.getLineOne().isBlank()) {
            message = "Address Line 1 is required!";
        } else if (userDTO.getLineTwo() == null || userDTO.getLineTwo().isBlank()) {
            message = "Address Line 2 is required!";
        } else if (userDTO.getCityId() <= 0) {
            message = "Please select a valid City!";
        } else if (userDTO.getPostalCode() == null || userDTO.getPostalCode().isBlank()) {
            message = "Postal Code is required!";
        } else if (!userDTO.getPostalCode().matches(Validator.POSTAL_CODE_VALIDATION)) {
            message = "Invalid Postal code!";
        } else {
            UserDTO sessionUser = (UserDTO) request.getSession().getAttribute("user");

            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = hibernateSession.beginTransaction();
                try {

                    User user = hibernateSession.get(User.class, sessionUser.getId());

                    City city = hibernateSession.get(City.class, userDTO.getCityId());
                    if (city == null) {
                        message = "Invalid City!";
                    }

                    Address address;

                    if (userDTO.getAddressId() != null && userDTO.getAddressId() > 0) {

                        address = hibernateSession.get(Address.class, userDTO.getAddressId());
                        if (address == null)
                            message = "Address not found!";
                        if (!address.getUser().getId().equals(user.getId()))
                            message = "You cannot edit this address!";
                        message = "Address updated successfully!";
                    } else {
                        // Check if address already exists (including deleted ones)
                        Address existingAddress = user.getAddresses().stream()
                                .filter(a -> a.getLineOne().equalsIgnoreCase(userDTO.getLineOne()) &&
                                        a.getLineTwo().equalsIgnoreCase(userDTO.getLineTwo()) &&
                                        a.getPostalCode().equalsIgnoreCase(userDTO.getPostalCode()) &&
                                        a.getCity().getId() == city.getId())
                                .findFirst()
                                .orElse(null);

                        if (existingAddress != null) {
                            address = existingAddress;
                            if (Boolean.TRUE.equals(address.getDeleted())) {
                                address.setDeleted(false);
                                message = "Restored previously deleted address!";
                            } else {
                                message = "Address updated successfully!";
                            }
                        } else {
                            // Create new
                            address = new Address();
                            address.setUser(user);
                            address.setIsPrimary((byte) 0);
                            message = "Address saved successfully!";
                        }
                    }

                    address.setLineOne(userDTO.getLineOne());
                    address.setLineTwo(userDTO.getLineTwo());
                    address.setPostalCode(userDTO.getPostalCode());
                    address.setCity(city);

                    if (userDTO.getAddressId() != null && userDTO.getAddressId() > 0) {
                        hibernateSession.merge(address);
                    } else {
                        hibernateSession.persist(address);
                    }

                    // Handle Make Primary
                    if (userDTO.isMakePrimary()) {
                        // Unset others
                        for (Address addr : user.getAddresses()) {
                            if (!addr.getId().equals(address.getId())) {
                                addr.setIsPrimary((byte) 0);
                                hibernateSession.merge(addr);
                            }
                        }
                        // Set current
                        address.setIsPrimary((byte) 1);
                        hibernateSession.merge(address);
                    } else if (address.getIsPrimary() == null) {
                        // Default to 0 if not set
                        address.setIsPrimary((byte) 0);
                    }

                    transaction.commit();

                    status = true;

                } catch (HibernateException e) {
                    transaction.rollback();
                    System.out.println("Hibernate Error:" + e.getMessage());

                    message = "Failed to save address. Please try again.";
                }
            }
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String logout(HttpServletRequest request, HttpServletResponse response) {
        JsonObject responseObject = new JsonObject();
        boolean status = true;
        String message = "Logged out successfully!";

        // Invalidate Session
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        // Invalidate Session
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        // Cookies are preserved (Remember Me) as per user request

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String deleteAddress(int id, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (request.getSession(false) == null || request.getSession().getAttribute("user") == null) {
            message = "Please sign in first!";
        } else {
            User sessionUser = (User) request.getSession().getAttribute("user");
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = hibernateSession.beginTransaction();
                try {
                    Address address = hibernateSession.get(Address.class, id);
                    if (address == null) {
                        message = "Address not found";
                    } else if (!address.getUser().getId().equals(sessionUser.getId())) {
                        message = "Unauthorized access";
                    } else {
                        address.setDeleted(true);
                        hibernateSession.merge(address);
                        transaction.commit();

                        status = true;
                        message = "Address deleted successfully";
                    }
                } catch (Exception e) {
                    if (transaction != null)
                        transaction.rollback();
                    e.printStackTrace();
                    message = "Error deleting address";
                }
            }
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String deleteMobile(int id, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (request.getSession(false) == null || request.getSession().getAttribute("user") == null) {
            message = "Please sign in first!";
        } else {
            User sessionUser = (User) request.getSession().getAttribute("user");
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = hibernateSession.beginTransaction();
                try {
                    Mobile mobile = hibernateSession.get(Mobile.class, id);
                    if (mobile == null) {
                        message = "Mobile number not found";
                    } else if (!mobile.getUser().getId().equals(sessionUser.getId())) {
                        message = "Unauthorized access";
                    } else {
                        // Check if this is the last mobile number
                        User user = hibernateSession.get(User.class, sessionUser.getId());
                        long activeMobileCount = user.getMobiles().stream()
                                .filter(m -> !m.getDeleted())
                                .count();

                        if (activeMobileCount <= 1) {
                            message = "You cannot delete your only mobile number.";
                        } else {
                            mobile.setDeleted(true);
                            hibernateSession.merge(mobile);
                            transaction.commit();

                            status = true;
                            message = "Mobile number deleted successfully";
                        }
                    }
                } catch (Exception e) {
                    if (transaction != null)
                        transaction.rollback();
                    e.printStackTrace();
                    message = "Error deleting mobile number";
                }
            }
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String saveMobile(UserDTO userDTO, HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message;

        if (request.getSession(false) == null || request.getSession().getAttribute("user") == null) {
            message = "Please sign in first!";
        } else if (userDTO.getMobile() == null || userDTO.getMobile().isBlank()) {
            message = "Mobile number is required!";
        } else if (!userDTO.getMobile().matches(Validator.MOBILE_VALIDATION)) {
            message = "Please provide valid mobile number!";
        } else {
            User sessionUser = (User) request.getSession().getAttribute("user");
            try (Session hibernateSession = HibernateUtil.getSessionFactory().openSession()) {
                Transaction transaction = hibernateSession.beginTransaction();
                try {
                    User user = hibernateSession.get(User.class, sessionUser.getId());

                    // Check for existing
                    Mobile existingMobile = user.getMobiles().stream()
                            .filter(m -> m.getMobile().equals(userDTO.getMobile()))
                            .findFirst()
                            .orElse(null);

                    if (existingMobile != null) {
                        if (Boolean.TRUE.equals(existingMobile.getDeleted())) {
                            existingMobile.setDeleted(false);
                            hibernateSession.merge(existingMobile);
                            transaction.commit();
                            status = true;
                            message = "Restored previously deleted mobile number!";
                        } else {
                            message = "Mobile number already exists!";
                        }
                    } else {
                        Mobile mobile = new Mobile();
                        mobile.setMobile(userDTO.getMobile());
                        mobile.setUser(user);
                        mobile.setDeleted(false);

                        hibernateSession.persist(mobile);
                        transaction.commit();
                        status = true;
                        message = "Mobile number added successfully!";
                    }

                } catch (Exception e) {
                    if (transaction != null)
                        transaction.rollback();
                    e.printStackTrace();
                    message = "Error saving mobile number";
                }
            }
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
