package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.entity.Gender;
import com.zentora.nike_x.entity.Mobile;
import com.zentora.nike_x.entity.Status;
import com.zentora.nike_x.entity.User;
import com.zentora.nike_x.mail.ResetPasswordMail;
import com.zentora.nike_x.mail.VerificationMail;
import com.zentora.nike_x.provider.MailServiceProvider;
import com.zentora.nike_x.util.AppUtil;
import com.zentora.nike_x.util.HibernateUtil;
import com.zentora.nike_x.util.SecurityUtil;
import com.zentora.nike_x.validation.Validator;
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
        } else if (userDTO.getGenderId() == null || !userDTO.getGenderId().equals(1) || userDTO.getGenderId().equals(2) || userDTO.getGenderId().equals(3)) {
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

                User newUser = hibernateSession.createNamedQuery("User.getByEmail", User.class).setParameter("email", userDTO.getNewEmail()).getSingleResultOrNull();
                if (singleUser == null) {
                    message = "Please Sign up first!";
                } else if (newUser != null) {
                    message = "An account already exists with the new email address provided!.";
                } else {

                    boolean isPasswordMatched = SecurityUtil.checkPassword(userDTO.getPassword(), singleUser.getPassword());

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

                            VerificationMail verificationMail = new VerificationMail(singleUser.getEmail(), verificationCode);
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

                        ResetPasswordMail resetPasswordMail =
                                new ResetPasswordMail(user.getEmail(), verificationCode);

                        MailServiceProvider.getMailServiceProvider().sendMail(resetPasswordMail);

                        status = true;
                        message = "A code to reset your password has been sent to your email.";
                    } catch (HibernateException e) {
                        if (transaction != null) transaction.rollback();
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
                        "                    \"The password must  8 to 25 characters long and include at least one uppercase letter, \" +\n" +
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
                    }else if(!userDTO.getVerificationCode().equals(user.getVerificationCode())){
                        message = "Invalid verification code!";
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
                            if (transaction != null) transaction.rollback();
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


}
