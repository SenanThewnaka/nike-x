document.addEventListener('DOMContentLoaded', () => {
    loadWishlistPage();
});

async function loadWishlistPage() {
    const grid = document.getElementById('wishlist-grid');
    const emptyState = document.getElementById('empty-state');
    const countBadge = document.getElementById('wishlist-count');

    if (!grid) return;

    try {
        const response = await fetch('../api/wishlist');
        const data = await response.json();

        if (data.status) {
            const items = data.items || [];

            // Update Count
            if (countBadge) countBadge.innerText = `${items.length} Item${items.length !== 1 ? 's' : ''}`;

            if (items.length === 0) {
                grid.classList.add('hidden');
                emptyState.classList.remove('hidden');
                emptyState.classList.add('flex');
            } else {
                grid.classList.remove('hidden');
                emptyState.classList.add('hidden');
                emptyState.classList.remove('flex');

                grid.innerHTML = items.map(product => {
                    const imagePath = product.imagePath ? product.imagePath : '../assets/images/placeholder.png';
                    // Fix path if it doesn't start with http or relative to root? 
                    // Usually image paths from DB are something like 'assets/images/...' 
                    // Since we are in 'user/', we might need to prepend '../' if the path is relative to root.
                    // For now, assuming the API returns paths relative to root context.
                    // If imagePath starts with 'assets/', prepend '../'.
                    const displayImage = imagePath.startsWith('assets/') ? '../' + imagePath : imagePath;

                    return `
                    <div class="group relative bg-white/[0.03] rounded-2xl overflow-hidden border border-white/5 hover:border-green-500/30 hover:shadow-[0_8px_40px_rgba(34,197,94,0.15)] transition-all duration-500 hover:-translate-y-2">
                        <!-- Remove Button -->
                        <button onclick="removeFromWishlistPage(${product.id}, this)" class="absolute top-3 right-3 z-20 w-8 h-8 rounded-full bg-black/40 backdrop-blur text-gray-400 hover:text-red-500 hover:bg-white flex items-center justify-center transition-all" title="Remove from Wishlist">
                            <i class="fas fa-times"></i>
                        </button>

                        <!-- Image -->
                        <div class="w-full aspect-[4/5] flex items-center justify-center bg-gradient-to-b from-white/[0.02] to-transparent p-6 cursor-pointer overflow-hidden" onclick="window.location.href='../product.html?id=${product.id}'">
                            <img src="${displayImage}" alt="${product.name}" class="max-w-full max-h-full object-contain rounded-2xl group-hover:scale-110 transition-transform duration-700 drop-shadow-2xl">
                        </div>

                        <!-- Content -->
                        <div class="p-5 border-t border-white/5">
                            <div class="cursor-pointer" onclick="window.location.href='../product.html?id=${product.id}'">
                                <p class="text-[10px] font-semibold text-gray-500 uppercase tracking-widest mb-1">${product.brand || ''}</p>
                                <h3 class="font-bold text-base text-white truncate mb-3 group-hover:text-green-400 transition-colors" title="${product.name}">${product.name}</h3>
                            </div>
                            <div class="flex items-center justify-between">
                                <span class="text-green-400 font-bold text-lg">Rs. ${product.price.toFixed(2)}</span>
                                <button onclick="addToCartFromWishlist(${product.id}, this)" 
                                    class="bg-green-600 hover:bg-green-500 text-white text-xs font-bold px-5 py-2.5 rounded-xl transition-all duration-300 hover:shadow-[0_4px_15px_rgba(34,197,94,0.4)]">
                                    ADD TO CART
                                </button>
                            </div>
                        </div>
                    </div>
                    `;
                }).join('');
            }

        } else {
            console.error(data.message);
            // Show error state?
            if (data.message.toLowerCase().includes("login")) {
                window.location.href = '../sign-in.html';
            }
        }

    } catch (e) {
        console.error("Error loading wishlist", e);
        grid.innerHTML = '<div class="col-span-full text-center text-red-400">Error loading wishlist. Please try again.</div>';
    }
}

async function removeFromWishlistPage(id, btn) {
    if (!confirm("Remove this item from your wishlist?")) return;

    // Optimistic UI: Find the card and fade it out
    const card = btn.closest('.glass-panel');

    try {
        const response = await fetch('../api/wishlist/remove', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: id })
        });
        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success("Item removed");
            // Remove element
            card.remove();

            // Check if empty
            const grid = document.getElementById('wishlist-grid');
            if (grid.children.length === 0) {
                loadWishlistPage(); // Trigger full reload to show empty state
            } else {
                // Update count separately if not reloading
                const countBadge = document.getElementById('wishlist-count');
                const currentCount = parseInt(countBadge.innerText);
                if (!isNaN(currentCount)) countBadge.innerText = `${currentCount - 1} Items`;
            }

            // Update Header
            if (window.loadHeaderWishlistItems) window.loadHeaderWishlistItems();

        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (e) {
        console.error(e);
        Notiflix.Notify.failure("Error removing item");
    }
}

async function addToCartFromWishlist(id, btn) {
    const originalText = btn.innerText;
    btn.innerText = 'Adding...';
    btn.disabled = true;

    try {
        const response = await fetch('../api/cart/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: id, qty: 1 })
        });
        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success("Added to Cart");
            if (window.updateCartCount) window.updateCartCount();
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (e) {
        console.error(e);
        Notiflix.Notify.failure("Failed to add to cart");
    } finally {
        btn.innerText = originalText;
        btn.disabled = false;
    }
}
