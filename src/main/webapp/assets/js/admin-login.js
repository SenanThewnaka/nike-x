async function sendCode() {
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();

    Notiflix.Loading.pulse("Sending Verification Code...", {
        clickToClose: false,
    });

    const requestBody = {
        email: email,
        password: password
    };

    try {
        const response = await fetch("../api/admin/send-code", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestBody)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Notify.success(data.message);

                // UI Transition
                document.getElementById("sendEmailBtn").classList.add("hidden");

                const verificationSection = document.getElementById("verification-section");
                verificationSection.classList.remove("hidden");

                const signInBtn = document.getElementById("signInBtn");
                signInBtn.classList.remove("hidden");
                signInBtn.classList.add("animate-fade-in-up");

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

async function signIn() {
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();
    const verificationCode = document.getElementById("verificationCode").value.trim();

    Notiflix.Loading.pulse("Authenticating...", {
        clickToClose: false,
    });

    const requestBody = {
        email: email,
        password: password,
        verificationCode: verificationCode
    };

    try {
        const response = await fetch("../api/admin/sign-in", {
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
                    'Access Granted',
                    data.message,
                    'Enter Portal',
                    () => {
                        window.location.href = 'dashboard.html';
                    }
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Authentication failed. Please try again.");
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
    } finally {
        Notiflix.Loading.remove();
    }
}
