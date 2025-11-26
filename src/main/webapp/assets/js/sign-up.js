async function signUp() {
    Notiflix.Loading.pulse("Wait...", {
        clickToClose: false,
        svgColor: '#0284c7'
    });


    let firstName = document.getElementById("firstName").value.trim();
    let lastName = document.getElementById("lastName").value.trim();
    let email = document.getElementById("email").value.trim();
    let mobile = document.getElementById("mobile").value.trim();
    let password = document.getElementById("password").value.trim();
    let genderId = document.getElementById("gender").value;

    const user = {
        firstName: firstName,
        lastName: lastName,
        email: email,
        mobile: mobile,
        password: password,
        genderId: genderId
    }
    try {
        const response = await fetch("api/users", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(user)
        });


        if (response.ok) { // 200
            const data = await response.json();
            if (data.status) {
                Notiflix.Report.success(
                    'SmartTrade',
                    data.message,
                    'Okay'
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure(response);
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    } finally {
        Notiflix.Loading.remove(1000);
    }
}