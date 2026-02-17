async function sendResetCode() {
    const email = document.getElementById("email").value.trim();

    Notiflix.Loading.pulse("Sending Code...", {
        clickToClose: false,
    });

    const user = {
        email: email
    }
    try {
        const response = await fetch("api/users/forgot-password", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(user)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Notify.success(data.message);

                document.getElementById("request-code-section").classList.add("hidden");
                document.getElementById("reset-password-section").classList.remove("hidden");
                document.getElementById("reset-password-section").classList.add("animate-slide-in-right");
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Something went wrong. Please try again.");
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
    } finally {
        Notiflix.Loading.remove();
    }
}

async function resetPassword() {
    const verificationCode = document.getElementById("verificationCode").value.trim();
    const newPassword = document.getElementById("newPassword").value.trim();
    const confirmPassword = document.getElementById("confirmPassword").value.trim();
    const email = document.getElementById("email").value.trim();

    Notiflix.Loading.pulse("Resetting Password...", {
        clickToClose: false,
    });

    const requestBody = {
        email: email,
        verificationCode: verificationCode,
        newPassword: newPassword,
        confirmPassword: confirmPassword
    };

    try {
        const response = await fetch("api/users/reset-password", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestBody)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Report.success(
                    'Password Reset',
                    data.message,
                    'Login Now',
                    () => {
                        window.location.href = 'sign-in.html';
                    }
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Something went wrong. Please try again.");
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
    } finally {
        Notiflix.Loading.remove();
    }
}


