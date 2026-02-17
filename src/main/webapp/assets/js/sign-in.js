async function signIn() {
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();
    const rememberMe = document.getElementById("rememberMeBox").checked;


    Notiflix.Loading.pulse("Signing In...", {
        clickToClose: false,
    });

    const user = {
        email: email,
        password: password,
        rememberMe: rememberMe
    };

    try {
        const response = await fetch("api/users/sign-in", {
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
                    'Welcome Back',
                    data.message,
                    'Okay',
                    () => {


                        window.location.href = 'index.html'; // Default fallback

                    }
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Invalid credentials. Please try again.");
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
    } finally {
        Notiflix.Loading.remove();
    }
}

window.onload = async function () {
    try {
        const response = await fetch("api/users/check-remember-me");
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                document.getElementById("email").value = data.email;
                document.getElementById("password").value = data.password;
                document.getElementById("rememberMeBox").checked = true;
            }
        }
    } catch (e) {
        console.error("Error checking remember me:", e);
    }
};
