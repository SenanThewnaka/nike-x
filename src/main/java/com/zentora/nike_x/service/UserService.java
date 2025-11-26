package com.zentora.nike_x.service;

import com.google.gson.JsonObject;
import com.zentora.nike_x.dto.UserDTO;
import com.zentora.nike_x.entity.Mobile;
import com.zentora.nike_x.entity.Status;
import com.zentora.nike_x.entity.User;
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
        }else if (userDTO.getMobile() == null) {
            message = "Mobile number is required!";
        } else if (userDTO.getMobile().isBlank()) {
            message = "Mobile number can not be empty!";
        } else if (!userDTO.getMobile().matches(Validator.MOBILE_VALIDATION)) {
            message = "Please provide valid mobile number!";
        }else if(!userDTO.getGenderId().equals(1) || userDTO.getStatusId().equals(2) || userDTO.getGenderId().equals(3)){
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

                String verificationCode = AppUtil.generateCode();

                u.setVerificationCode(verificationCode);

                Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.PENDING)).getSingleResult();

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
                    MailServiceProvider.getInstance().sendMail(verificationMail);
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


}
