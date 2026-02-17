async function loadUserProfile() {
    try {
        const response = await fetch('../api/users/profile');
        if (!response.ok) throw new Error("Failed to fetch profile");

        const data = await response.json();

        if (data.status) {
            const user = data.user;

            // Personal Info
            const firstNameEl = document.getElementById('profile-first-name');
            const lastNameEl = document.getElementById('profile-last-name');
            const emailEl = document.getElementById('profile-email');

            if (firstNameEl) firstNameEl.value = user.firstName;
            if (lastNameEl) lastNameEl.value = user.lastName;
            if (emailEl) emailEl.value = user.email;

            // Header/Sidebar Display
            const initials = (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
            const fullName = `${user.firstName} ${user.lastName}`;

            const initialsEl = document.getElementById('profile-initials');
            const nameDisplayEl = document.getElementById('profile-name-display');
            const joinedDateEl = document.getElementById('profile-joined-date');

            if (initialsEl) initialsEl.innerText = initials;
            if (nameDisplayEl) nameDisplayEl.innerText = fullName;
            if (joinedDateEl) joinedDateEl.innerText = `Member since ${user.registeredDate}`;

            // Mobiles
            const mobileContainer = document.getElementById('mobile-container');
            if (mobileContainer) {
                mobileContainer.innerHTML = ''; // Clear static
                user.mobiles.forEach(m => {
                    const mobileHTML = `
                        <div class="flex gap-2">
                            <input type="tel" value="${m.mobile}" readonly
                                class="flex-1 glass-morphism bg-transparent border-gray-700/50 rounded-xl px-5 py-3 focus:outline-none focus:border-green-500 transition-colors text-white placeholder-gray-600">
                            <button type="button" onclick="deleteMobile(${m.id})"
                                class="w-12 glass-morphism border-gray-700/50 rounded-xl text-red-400 hover:bg-red-500/10 hover:text-red-500 transition-colors">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    `;
                    mobileContainer.insertAdjacentHTML('beforeend', mobileHTML);
                });
            }

            // Addresses
            const addressContainer = document.getElementById('address-container');
            if (addressContainer) {
                addressContainer.innerHTML = ''; // Clear static
                if (user.addresses.length === 0) {
                    addressContainer.innerHTML = '<p class="text-gray-500 text-sm col-span-2 text-center py-4">No addresses saved yet.</p>';
                } else {
                    user.addresses.forEach(a => {
                        const addressData = JSON.stringify(a).replace(/"/g, '&quot;');

                        let primaryBadge = '';
                        let makePrimaryBtn = '';

                        if (a.isPrimary) {
                            primaryBadge = `<span class="absolute top-4 left-4 bg-green-500/20 text-green-400 text-[10px] font-bold px-2 py-1 rounded-full border border-green-500/30">PRIMARY</span>`;
                        } else {
                            makePrimaryBtn = `
                                <button type="button" onclick="setPrimaryAddress(${a.id})" 
                                    class="text-xs text-gray-500 hover:text-green-400 font-medium transition-colors mt-2 flex items-center gap-1">
                                    <i class="fas fa-check-circle"></i> Make Primary
                                </button>
                            `;
                        }

                        const addressHTML = `
                             <div class="glass-panel border ${a.isPrimary ? 'border-green-500/30 bg-green-500/5' : 'border-white/5'} p-4 rounded-xl relative group hover:bg-white/5 transition-colors cursor-pointer">
                                ${primaryBadge}
                                <div class="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button type="button" onclick="openAddressModal(${addressData})" class="text-xs text-gray-400 hover:text-white">
                                        <i class="fas fa-edit"></i>
                                    </button>
                                    <button type="button" onclick="deleteAddress(${a.id})" class="text-xs text-red-400 hover:text-red-500"><i class="fas fa-trash"></i></button>
                                </div>
                                <span class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2 block ${a.isPrimary ? 'mt-6' : ''}">Address</span>
                                <p class="text-sm text-gray-300 leading-relaxed">
                                    ${a.lineOne},<br>${a.lineTwo},<br>${a.city} ${a.postalCode ? '- ' + a.postalCode : ''}
                                </p>
                                ${makePrimaryBtn}
                            </div>
                        `;
                        addressContainer.insertAdjacentHTML('beforeend', addressHTML);
                    });
                }
            }

        } else {
            // Not logged in or error
            // For now, maybe just redirect or show alert
            console.log("Profile load failed: " + data.message);
        }

    } catch (e) {
        console.error("Error loading profile", e);
    }
}

async function deleteMobile(id) {
    Notiflix.Confirm.show(
        'Delete Mobile Number',
        'Are you sure you want to remove this number?',
        'Yes',
        'No',
        async function () {
            try {
                Notiflix.Loading.pulse('Deleting...');
                const response = await fetch(`../api/users/delete-mobile?id=${id}`);
                const data = await response.json();
                if (data.status) {
                    Notiflix.Notify.success(data.message);
                    loadUserProfile();
                } else {
                    Notiflix.Notify.failure(data.message);
                }
            } catch (e) {
                console.error("Error deleting mobile", e);
                Notiflix.Notify.failure("An error occurred while deleting the mobile number.");
            } finally {
                Notiflix.Loading.remove();
            }
        }
    );
}

async function deleteAddress(id) {
    Notiflix.Confirm.show(
        'Delete Address',
        'Are you sure you want to remove this address?',
        'Yes',
        'No',
        async function () {
            try {
                Notiflix.Loading.pulse('Deleting...');
                const response = await fetch(`../api/users/delete-address?id=${id}`);
                const data = await response.json();
                if (data.status) {
                    Notiflix.Notify.success(data.message);
                    loadUserProfile();
                } else {
                    Notiflix.Notify.failure(data.message);
                }
            } catch (e) {
                console.error("Error deleting address", e);
                Notiflix.Notify.failure("An error occurred while deleting the address.");
            } finally {
                Notiflix.Loading.remove();
            }
        }
    );
}

async function loadCities() {
    try {
        const response = await fetch('../api/cities');
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                const citySelect = document.getElementById('addr-city');
                data.cities.forEach(city => {
                    const option = document.createElement('option');
                    option.value = city.id;
                    option.textContent = city.name;
                    citySelect.appendChild(option);
                });
            }
        }
    } catch (e) {
        console.error("Error loading cities", e);
    }
}

async function saveAddress(event) {
    event.preventDefault();

    const id = document.getElementById('addr-id').value;
    const lineOne = document.getElementById('addr-line1').value;
    const lineTwo = document.getElementById('addr-line2').value;
    const cityId = document.getElementById('addr-city').value;
    const postalCode = document.getElementById('addr-postal').value;
    const makePrimary = document.getElementById('addr-primary').checked;

    const payload = {
        addressId: id ? parseInt(id) : null,
        lineOne: lineOne,
        lineTwo: lineTwo,
        cityId: parseInt(cityId),
        postalCode: postalCode,
        makePrimary: makePrimary
    };

    try {
        Notiflix.Loading.pulse('Saving Address...');
        const response = await fetch('../api/users/save-address', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (data.status) {
            closeModal('address-modal');
            Notiflix.Notify.success(data.message);
            loadUserProfile(); // Refresh list
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (e) {
        console.error("Error saving address", e);
        Notiflix.Notify.failure("Something went wrong. Please try again.");
    } finally {
        Notiflix.Loading.remove();
    }
}

async function setPrimaryAddress(id) {
    try {
        const response = await fetch(`../api/users/set-primary-address?id=${id}`);
        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success(data.message);
            loadUserProfile(); // Reload to reflect changes
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (e) {
        console.error("Error setting primary address", e);
        Notiflix.Notify.failure("Error setting primary address");
    }
}

function openAddressModal(address = null) {
    const modal = document.getElementById('address-modal');
    const title = document.getElementById('address-modal-title');
    const idField = document.getElementById('addr-id');
    const line1Field = document.getElementById('addr-line1');
    const line2Field = document.getElementById('addr-line2');
    const cityField = document.getElementById('addr-city');
    const postalField = document.getElementById('addr-postal');
    const primaryField = document.getElementById('addr-primary');

    if (modal) {
        modal.classList.remove('hidden');
        if (address) {
            // Edit Mode
            title.innerText = 'Edit Address';
            idField.value = address.id;
            line1Field.value = address.lineOne;
            line2Field.value = address.lineTwo;
            postalField.value = address.postalCode;
            // Set City (ensure types match, usually string in value)
            if (address.cityId) cityField.value = address.cityId;

            // Set Primary Checkbox
            primaryField.checked = !!address.isPrimary;

        } else {
            // Add Mode
            title.innerText = 'Add New Address';
            idField.value = '';
            line1Field.value = '';
            line2Field.value = '';
            cityField.value = ''; // Reset select
            postalField.value = '';
            primaryField.checked = false;
        }
    }
}

// Override generic openModal for addresses or update button onclicks
// Easier to just update the button onclicks in the generated HTML


// function calls moved to profile.html to prevent double execution
