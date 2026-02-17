// Cart Page Logic

document.addEventListener('DOMContentLoaded', () => {
    loadCart();
});

let cartItems = [];

async function loadCart() {
    const container = document.getElementById('cart-items-container');

    try {
        const response = await fetch('api/cart');
        if (!response.ok) throw new Error('Failed to load cart');

        cartItems = await response.json();
        renderCart();
        updateSummary();

    } catch (error) {
        console.error('Error loading cart:', error);
        container.innerHTML = `
            <div class="glass-panel p-8 rounded-3xl text-center">
                <p class="text-red-400">Failed to load your cart. Please try again.</p>
                <button onclick="loadCart()" class="mt-4 text-green-500 hover:text-green-400">Retry</button>
            </div>
        `;
    }
}

function renderCart() {
    const container = document.getElementById('cart-items-container');

    if (cartItems.length === 0) {
        renderEmptyState(container);
        return;
    }

    container.innerHTML = cartItems.map(item => `
        <div class="glass-panel p-6 rounded-3xl flex flex-col md:flex-row gap-6 items-center" id="cart-item-${item.stockId}">
            <!-- Product Image -->
            <a href="product.html?id=${item.productId}&size=${item.sizeId}&color=${item.colorId}&from=cart" class="w-24 h-24 md:w-32 md:h-32 bg-white/5 rounded-2xl flex-shrink-0 p-2 hover:bg-white/10 transition-colors cursor-pointer">
                <img src="${item.imagePath || 'assets/images/products/placeholder.png'}" 
                     alt="${item.productName}" 
                     class="w-full h-full object-contain mix-blend-lighten filter drop-shadow-[0_5px_15px_rgba(0,0,0,0.5)]">
            </a>
            
            <!-- Details -->
            <a href="product.html?id=${item.productId}&size=${item.sizeId}&color=${item.colorId}&from=cart" class="flex-grow text-center md:text-left hover:opacity-80 transition-opacity">
                <h3 class="text-xl font-bold mb-1">${item.productName}</h3>
                <div class="flex items-center justify-center md:justify-start gap-3 text-sm text-gray-400 mb-3">
                    <span>${item.sizeName}</span>
                    <span class="w-1 h-1 bg-gray-600 rounded-full"></span>
                    <div class="flex items-center gap-1">
                        <span class="w-3 h-3 rounded-full border border-white/20" style="background-color: ${item.colorHex}"></span>
                        <span>${item.colorName}</span>
                    </div>
                </div>
                <div class="text-green-500 font-bold text-lg">Rs. ${item.price.toFixed(2)}</div>
            </a>
            
            <!-- Actions -->
            <div class="flex items-center gap-6">
                <!-- Quantity -->
                <div class="flex items-center bg-white/5 rounded-xl border border-white/10">
                    <button onclick="updateQuantity(${item.stockId}, ${item.qty}, -1)" 
                            class="w-10 h-10 flex items-center justify-center hover:bg-white/10 transition-colors text-gray-400 hover:text-white rounded-l-xl">
                        <i class="fas fa-minus text-xs"></i>
                    </button>
                    <span class="w-10 text-center font-bold">${item.qty}</span>
                    <button onclick="updateQuantity(${item.stockId}, ${item.qty}, 1)"
                            class="w-10 h-10 flex items-center justify-center hover:bg-white/10 transition-colors text-gray-400 hover:text-white rounded-r-xl">
                        <i class="fas fa-plus text-xs"></i>
                    </button>
                </div>
                
                <!-- Remove -->
                <button onclick="removeItem(${item.stockId})" 
                        class="w-10 h-10 flex items-center justify-center rounded-xl bg-red-500/10 text-red-500 hover:bg-red-500 hover:text-white transition-all">
                    <i class="fas fa-trash-alt"></i>
                </button>
            </div>
        </div>
    `).join('');
}

function renderEmptyState(container) {
    container.innerHTML = `
        <div class="glass-panel p-12 rounded-3xl text-center">
            <div class="w-20 h-20 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-6">
                <i class="fas fa-shopping-cart text-3xl text-gray-600"></i>
            </div>
            <h3 class="text-2xl font-bold mb-2">No items in the cart</h3>
            <p class="text-gray-400 mb-8">Add something to make me happy :)</p>
            <a href="index.html" class="inline-block bg-green-600 hover:bg-green-700 text-white px-8 py-3 rounded-full font-bold transition-all shadow-[0_0_20px_rgba(34,197,94,0.3)]">
                Continue Shopping
            </a>
        </div>
    `;

    // Also disable checkout
    const checkoutBtn = document.querySelector('button[onclick="proceedToCheckout()"]');
    if (checkoutBtn) {
        checkoutBtn.disabled = true;
        checkoutBtn.classList.add('opacity-50', 'cursor-not-allowed');
        checkoutBtn.classList.remove('hover:bg-green-700', 'hover:shadow-[0_0_30px_rgba(34,197,94,0.5)]');
    }
}

async function updateQuantity(stockId, currentQty, change) {
    const newQty = currentQty + change;

    if (newQty < 1) return; // Prevent 0 qty here, use remove button for that

    try {
        const response = await fetch('api/cart/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ stockId, qty: newQty })
        });

        const result = await response.json();

        if (result.status) {
            // Update local state and re-render partial or full
            // For simplicity, just reload cart data or update local array
            const item = cartItems.find(i => i.stockId === stockId);
            if (item) {
                item.qty = newQty;
                renderCart(); // Re-render to update inputs/totals
                updateSummary();
                // Update badge globally
                if (window.updateCartCount) window.updateCartCount();
            }
        } else {
            Notiflix.Notify.failure(result.message || 'Failed to update quantity');
        }
    } catch (error) {
        console.error('Error updating quantity:', error);
        Notiflix.Notify.failure('Connection error');
    }
}

async function removeItem(stockId) {
    Notiflix.Confirm.show(
        'Remove Item',
        'Are you sure you want to remove this item?',
        'Yes',
        'No',
        async () => {
            try {
                const response = await fetch('api/cart/remove', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ stockId })
                });

                const result = await response.json();

                if (result.status) {
                    Notiflix.Notify.success('Item removed');
                    cartItems = cartItems.filter(i => i.stockId !== stockId);
                    renderCart();
                    updateSummary();
                    if (window.updateCartCount) window.updateCartCount();
                } else {
                    Notiflix.Notify.failure(result.message || 'Failed to remove item');
                }
            } catch (error) {
                Notiflix.Notify.failure('Connection error');
            }
        }
    );
}

function updateSummary() {
    const subtotal = cartItems.reduce((sum, item) => sum + (item.price * item.qty), 0);
    const shipping = subtotal > 0 ? 0 : 0; // Free shipping logic for now
    const total = subtotal + shipping;

    document.getElementById('summary-subtotal').textContent = `Rs. ${subtotal.toFixed(2)}`;
    document.getElementById('summary-total').textContent = `Rs. ${total.toFixed(2)}`;

    // Update "Shipping" text if we want logic later
    const shippingEl = document.getElementById('summary-shipping');
    shippingEl.textContent = 'Free';
    shippingEl.classList.add('text-green-500');
}

function proceedToCheckout() {
    if (cartItems.length === 0) {
        Notiflix.Notify.warning('Your cart is empty!');
        return;
    }
    // Redirect to checkout page
    window.location.href = 'checkout.html';
}
