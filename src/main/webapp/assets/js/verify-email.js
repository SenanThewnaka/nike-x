let params = new URLSearchParams(window.location.search);
let email = params.get("email");







async function updateEmail() {
    Notiflix.Loading.pulse("Wait...", {
        clickToClose: false,
    });

    let currentEmail = document.getElementById("currentEmail").value.trim();
    let newEmail = document.getElementById("newEmail").value.trim();
    let password = document.getElementById("password").value.trim();

    const user = {
        email: currentEmail,
        newEmail: newEmail,
        password: password
    }

    try {

        const response = await fetch("api/users/update-email", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(user)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Report.success(
                    'Nike-X',
                    data.message,
                    'Okay',
                    () => {
                        window.location.href = 'verify-email.html?email=' + newEmail;
                    }
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Something went wrong. Please try again later.");
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
    } finally {
        Notiflix.Loading.remove();
    }
}

async function resendEmail() {
    Notiflix.Loading.pulse("Sending...", {
        clickToClose: false,
    });

    const user = {
        email: email
    }
    try {
        const response = await fetch("api/users/resend-code", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(user)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Report.success(
                    'Nike-X',
                    data.message,
                    'Okay'
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Failed to resend email. Please try again.");
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
    } finally {
        Notiflix.Loading.remove();
    }
}

async function verifyEmail() {
    Notiflix.Loading.pulse("Checking...", {
        clickToClose: false,
    });

    const verificationCode = document.getElementById("verificationCode").value.trim();

    const user = {
        email: email,
        verificationCode: verificationCode
    }
    try {
        const response = await fetch("api/users/sign-in.html", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(user)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Report.success(
                    'Nike-X',
                    data.message,
                    'Okay',
                    () => {
                        window.location.href = 'index.html';
                    }
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Something went wrong. Please try again later.");
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
    } finally {
        Notiflix.Loading.remove();
    }
}