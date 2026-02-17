const basePath = getBasePath();

document.addEventListener('DOMContentLoaded', async () => {
    await checkAuthCheckout();
    loadCheckoutData();
    loadCities(); // For address modal
});

function getBasePath() {
    const path = window.location.pathname;
    if (path.includes('/nike-x/')) {
        return '/nike-x/';
    }
    return '/';
}

async function checkAuthCheckout() {
    try {
        const response = await fetch(`${basePath}api/auth/status`);
        if (response.ok) {
            const data = await response.json();
            if (!data.isLoggedIn) {
                window.location.href = `${basePath}sign-in.html`;
            }
        } else {
            // If 500 or 404, assume not logged in or error
            window.location.href = `${basePath}sign-in.html`;
        }
    } catch (e) {
        console.error("Auth check failed", e);
        window.location.href = `${basePath}sign-in.html`;
    }
}

let cartTotal = 0;
let cartItems = [];

async function loadCheckoutData() {
    try {
        // Check for Buy Now mode (specific stockId)
        const urlParams = new URLSearchParams(window.location.search);
        const buyNowStockId = urlParams.get('stockId');

        // Define requests map
        const requests = [
            fetch(`${basePath}api/users/profile`) // Index 0 (Profile) always needed
        ];

        if (buyNowStockId) {
            // Buy Now Mode: Fetch single stock details
            requests.push(fetch(`${basePath}api/products/stock?id=${buyNowStockId}`));
        } else {
            // Normal Mode: Fetch Cart
            requests.push(fetch(`${basePath}api/cart`));
        }

        const responses = await Promise.all(requests);
        const profileRes = responses[0];
        const itemRes = responses[1]; // Either Stock or Cart

        // Process Item Response (Cart or Single Stock)
        if (itemRes.ok) {
            const data = await itemRes.json();

            if (buyNowStockId) {
                // Single Stock Item
                if (data) {
                    // Apply Qty from URL or default to 1
                    const qty = urlParams.get('qty') ? parseInt(urlParams.get('qty')) : 1;
                    data.qty = qty;
                    cartItems = [data]; // Wrap in array
                }
            } else {
                // Full Cart Data
                let allItems = [];
                if (Array.isArray(data)) {
                    allItems = data;
                } else if (data.status && data.cartItems) {
                    allItems = data.cartItems;
                }
                cartItems = allItems;
            }

            renderOrderSummary(cartItems);

        } else {
            console.error("Item data fetch failed", itemRes.status);
            if (buyNowStockId) {
                Notiflix.Notify.failure("Failed to load selected product.");
            }
        }

        if (profileRes.ok) {
            // ... existing profile logic ...

            const profileData = await profileRes.json();
            if (profileData.status && profileData.user) {
                renderAddresses(profileData.user.addresses || []);
                renderMobiles(profileData.user.mobiles || []);
            } else {
                console.warn("Profile loaded but status false or user missing", profileData);
                // Render empty to avoid crash
                renderAddresses([]);
                renderMobiles([]);
            }
        } else {
            console.error("Profile API failed", profileRes.status);
        }

    } catch (e) {
        console.error("Error loading checkout data", e);
        Notiflix.Notify.failure("Failed to load checkout data. Check console.");
    }
}

function renderOrderSummary(items) {
    const container = document.getElementById('checkout-items-container');
    container.innerHTML = '';
    cartTotal = 0;

    if (!items || items.length === 0) {
        container.innerHTML = '<p class="text-center text-gray-400">Your cart is empty.</p>';
        return;
    }

    items.forEach(item => {
        // Properties match CartItemDTO (flat structure)
        const price = item.price || 0;
        const qty = item.qty || 0;
        const itemTotal = price * qty;
        cartTotal += itemTotal;

        const imagePath = item.imagePath || 'assets/images/products/placeholder.png';

        const html = `
            <div class="flex items-center justify-between p-2 bg-white/5 rounded-xl">
                <div class="flex items-center gap-3">
                    <img src="${basePath}${imagePath}" alt="${item.productName}" class="w-12 h-12 rounded-lg object-cover">
                    <div class="text-sm">
                        <p class="font-bold text-white line-clamp-1">${item.productName}</p>
                        <p class="text-gray-400 text-xs">${item.colorName} / ${item.sizeName} x ${qty}</p>
                    </div>
                </div>
                <div class="text-right">
                    <p class="font-bold text-green-500">Rs. ${itemTotal.toFixed(2)}</p>
                </div>
            </div>
        `;
        container.innerHTML += html;
    });

    updateTotals();
}

function updateTotals() {
    document.getElementById('summary-subtotal').textContent = `Rs. ${cartTotal.toFixed(2)}`;
    document.getElementById('summary-total').textContent = `Rs. ${cartTotal.toFixed(2)}`;
}

function renderAddresses(addresses) {
    const container = document.getElementById('address-list-container');
    container.innerHTML = '';

    if (!addresses || addresses.length === 0) {
        container.innerHTML = `
            <div class="text-center p-6 border-2 border-dashed border-white/10 rounded-xl">
                <p class="text-gray-400 mb-2">No addresses found</p>
                <button onclick="openAddressModal()" class="text-green-500 hover:text-green-400 font-bold">Add New Address</button>
            </div>
        `;
        return;
    }

    addresses.forEach((addr, index) => {
        const isPrimary = addr.isPrimary;
        const html = `
            <label class="block relative cursor-pointer group">
                <input type="radio" name="shipping_address" value="${addr.id}" class="peer sr-only" ${index === 0 ? 'checked' : ''}>
                <div class="p-4 rounded-xl bg-white/5 border border-white/10 peer-checked:border-green-500 peer-checked:bg-green-500/10 transition-all flex items-start justify-between">
                    <div>
                        <div class="flex items-center gap-2 mb-1">
                            <h3 class="font-bold text-white">Address #${index + 1}</h3>
                            ${isPrimary ? '<span class="text-[10px] bg-green-500/20 text-green-500 px-2 py-0.5 rounded-full uppercase tracking-wider font-bold">Primary</span>' : ''}
                        </div>
                        <p class="text-sm text-gray-300 text-left">${addr.lineOne}, ${addr.lineTwo}</p>
                        <p class="text-sm text-gray-300 text-left">${addr.city}, ${addr.postalCode}</p>
                    </div>
                    <div class="w-5 h-5 rounded-full border-2 border-gray-500 peer-checked:border-green-500 peer-checked:bg-green-500 relative flex items-center justify-center mt-1">
                        <i class="fas fa-check text-black text-[10px] opacity-0 peer-checked:opacity-100"></i>
                    </div>
                </div>
            </label>
        `;
        container.innerHTML += html;
    });
}

async function placeOrder() {
    const addressRadio = document.querySelector('input[name="shipping_address"]:checked');
    const mobileRadio = document.querySelector('input[name="contact_mobile"]:checked');
    const addressId = addressRadio.value;
    const mobileNumber = mobileRadio.value;
    const paymentMethod = 'Card'; // Hardcoded as per request (No COD)

    const btn = document.getElementById('place-order-btn');
    const originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i> Processing...';

    const urlParams = new URLSearchParams(window.location.search);
    const buyNowStockId = urlParams.get('stockId');

    try {
        const payload = {
            addressId: addressId,
            paymentMethod: paymentMethod,
            mobile: mobileNumber
        };

        if (buyNowStockId) {
            payload.stockId = parseInt(buyNowStockId);
        }

        const response = await fetch(`${basePath}api/order/checkout`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const textResponse = await response.text();
        console.log("Raw Response:", textResponse);

        let data;
        try {
            data = JSON.parse(textResponse);
        } catch (parseError) {
            console.error("JSON Parse Error", parseError);
            Notiflix.Report.failure('Server Error', 'Invalid Server Response: ' + textResponse.substring(0, 150) + '...', 'Okay');
            btn.disabled = false;
            btn.innerHTML = originalText;
            return;
        }

        if (data.status) {

            if (data.payhereParams) {
                // Launch PayHere
                payhere.onCompleted = async function onCompleted(orderId) {
                    console.log("PayHere completed. Verifying payment logic...");
                    Notiflix.Loading.pulse('Verifying Payment...');

                    let isPaid = false;
                    let attempts = 0;
                    const maxAttempts = 6; // Poll for ~12 seconds

                    // Poll for PAID status
                    while (attempts < maxAttempts) {
                        try {
                            const res = await fetch(`${basePath}api/order/details?id=${orderId}`);
                            const json = await res.json();
                            if (json.status && json.order && json.order.status === 'PAID') {
                                isPaid = true;
                                break;
                            }
                        } catch (e) { console.error(e); }

                        attempts++;
                        await new Promise(r => setTimeout(r, 2000));
                    }

                    Notiflix.Loading.remove();

                    if (isPaid) {
                        Notiflix.Report.success(
                            'Payment Successful!',
                            'Your order #' + orderId + ' has been confirmed.',
                            'View Invoice',
                            () => window.location.href = `${basePath}user/invoice.html?id=${orderId}`
                        );
                    } else {
                        // Strict Requirement: If money not verified, consider FAILED and DELETE.
                        console.warn("Payment verification timed out. Deleting order...");
                        notifyPaymentFailure(orderId); // Helper triggers backend deletion

                        Notiflix.Report.failure(
                            'Order Failed',
                            'We could not verify your payment. The order has been cancelled.',
                            'Okay',
                            () => window.location.reload()
                        );
                    }
                };

                payhere.onDismissed = function onDismissed() {
                    Notiflix.Notify.warning('Payment dismissed.');
                    // Rollback Order
                    notifyPaymentFailure(data.orderId || data.payhereParams.order_id);
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                };

                payhere.onError = function onError(error) {
                    console.error("PayHere Error", error);
                    Notiflix.Report.failure('Payment Error', 'An error occurred during payment: ' + error, 'Okay');
                    // Rollback Order
                    notifyPaymentFailure(data.orderId || data.payhereParams.order_id);
                    btn.disabled = false;
                    btn.innerHTML = originalText;
                };

                payhere.startPayment(data.payhereParams);

            } else {
                // Normal Success (COD)
                const oid = data.orderId;
                Notiflix.Report.success(
                    'Order Placed!',
                    data.message || 'Your order has been placed successfully.',
                    'View Invoice',
                    () => {
                        if (oid) {
                            window.location.href = `${basePath}user/invoice.html?id=${oid}`;
                        } else {
                            window.location.href = `${basePath}index.html`;
                        }
                    }
                );
            }

        } else {
            Notiflix.Report.failure('Order Failed', data.message || 'Something went wrong.', 'Okay');
            btn.disabled = false;
            btn.innerHTML = originalText;
        }

    } catch (e) {
        console.error("Order error", e);
        Notiflix.Report.failure('Error', 'Failed to place order. Check console/network.', 'Okay');
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

// --- Address Modal Logic (Simplified Reuse) ---

function openAddressModal() {
    document.getElementById('address-modal').classList.remove('hidden');
    // Load cities if not loaded? handled in init
}

function closeAddressModal() {
    document.getElementById('address-modal').classList.add('hidden');
    document.getElementById('address-form').reset();
}

async function loadCities() {
    try {
        const response = await fetch(`${basePath}api/city/list`);
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                const select = document.getElementById('city');
                // Keep default option
                select.innerHTML = '<option value="" class="bg-zinc-900">Select City</option>';
                data.cities.forEach(city => {
                    const option = document.createElement('option');
                    option.value = city.id;
                    option.textContent = city.name;
                    option.className = 'bg-zinc-900';
                    select.appendChild(option);
                });
            }
        }
    } catch (e) {
        console.error("Failed to load cities", e);
    }
}

async function saveAddress(event) {
    event.preventDefault();

    // Gather data
    const line1 = document.getElementById('line1').value;
    const line2 = document.getElementById('line2').value;
    const cityId = document.getElementById('city').value;
    const postal = document.getElementById('postal').value;
    const makePrimary = document.getElementById('make-primary').checked;

    const addressData = {
        lineOne: line1,
        lineTwo: line2,
        cityId: parseInt(cityId),
        postalCode: postal,
        makePrimary: makePrimary
    };

    try {
        const response = await fetch(`${basePath}api/users/save-address`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(addressData)
        });

        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success("Address added successfully!");
            closeAddressModal();
            loadCheckoutData(); // Reload addresses
        } else {
            Notiflix.Report.failure("Error", data.message, "Okay");
        }

    } catch (e) {
        Notiflix.Report.failure("Error", "Failed to save address", "Okay");
    }
}

// --- Mobile Modal & Logic ---

function renderMobiles(mobiles) {
    const container = document.getElementById('mobile-list-container');
    container.innerHTML = '';

    if (!mobiles || mobiles.length === 0) {
        container.innerHTML = `
            <div class="text-center p-6 border-2 border-dashed border-white/10 rounded-xl">
                <p class="text-gray-400 mb-2">No numbers found</p>
                <button onclick="openMobileModal()" class="text-green-500 hover:text-green-400 font-bold">Add New Number</button>
            </div>
        `;
        return;
    }

    mobiles.forEach((m, index) => {
        const html = `
            <label class="block relative cursor-pointer group">
                <input type="radio" name="contact_mobile" value="${m.mobile}" class="peer sr-only" ${index === 0 ? 'checked' : ''}>
                <div class="p-4 rounded-xl bg-white/5 border border-white/10 peer-checked:border-green-500 peer-checked:bg-green-500/10 transition-all flex items-center justify-between">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
                            <i class="fas fa-phone text-gray-400"></i>
                        </div>
                        <div>
                            <p class="font-mono text-lg text-white tracking-wider">${m.mobile}</p>
                            <p class="text-xs text-gray-500">Saved Number</p>
                        </div>
                    </div>
                    <div class="w-5 h-5 rounded-full border-2 border-gray-500 peer-checked:border-green-500 peer-checked:bg-green-500 relative flex items-center justify-center">
                        <i class="fas fa-check text-black text-[10px] opacity-0 peer-checked:opacity-100"></i>
                    </div>
                </div>
            </label>
        `;
        container.innerHTML += html;
    });
}

function openMobileModal() {
    document.getElementById('mobile-modal').classList.remove('hidden');
}

function closeMobileModal() {
    document.getElementById('mobile-modal').classList.add('hidden');
    document.getElementById('mobile-form').reset();
}

async function saveMobile(event) {
    event.preventDefault();
    const mobileInput = document.getElementById('mobile-input');
    const mobile = mobileInput.value;

    if (!mobile.match(/^07[01245678][0-9]{7}$/)) {
        Notiflix.Notify.failure("Invalid mobile number format!");
        return;
    }

    try {
        const response = await fetch(`${basePath}api/users/save-mobile`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mobile: mobile })
        });

        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success("Mobile number saved!");
            closeMobileModal();
            loadCheckoutData(); // Reload to show new number
        } else {
            Notiflix.Report.failure("Error", data.message, "Okay");
        }

    } catch (e) {
        console.error("Save mobile error", e);
        Notiflix.Report.failure("Error", "Failed to save mobile number", "Okay");
    }
}

// Helper to clean up pending order if payment fails
function notifyPaymentFailure(orderId) {
    if (!orderId) return;

    // Check if orderId is inside params object (common PayHere pattern)
    // Actually the arg passed is usually the ID itself

    const formData = new URLSearchParams();
    formData.append('orderId', orderId);

    fetch('api/order/payment-failed', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: formData
    })
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                console.log("Pending order rolled back successfully.");
            } else {
                console.warn("Could not rollback pending order:", data.message);
            }
        })
        .catch(err => console.error("Error verifying payment failure", err));
}
