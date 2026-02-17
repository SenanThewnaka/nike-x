const API_BASE_URL = 'api';

let currentState = {
    query: '',
    minPrice: null,
    maxPrice: null,
    brandIds: [],
    sizeIds: [],
    colorIds: [],
    sort: 'NEWEST',
    page: 1,
    pageSize: 12
};

document.addEventListener('DOMContentLoaded', () => {
    loadFilters();

    // Read query param from URL (e.g. from header search redirect)
    const urlParams = new URLSearchParams(window.location.search);
    const urlQuery = urlParams.get('query');
    if (urlQuery) {
        currentState.query = urlQuery;
        const searchInput = document.getElementById('search-input');
        if (searchInput) {
            searchInput.value = urlQuery;
        }
    }

    // Initial Search
    searchProducts();

    // Event Listeners
    setupEventListeners();
});

async function loadFilters() {
    try {
        const [brandsRes, sizesRes, colorsRes] = await Promise.all([
            fetch(`${API_BASE_URL}/products/brands`),
            fetch(`${API_BASE_URL}/products/sizes`),
            fetch(`${API_BASE_URL}/products/colors`)
        ]);

        if (!brandsRes.ok || !sizesRes.ok || !colorsRes.ok) {
            throw new Error("One or more filter requests failed");
        }

        const brandsData = await brandsRes.json();
        const sizesData = await sizesRes.json();
        const colorsData = await colorsRes.json();

        if (brandsData.status) renderBrands(brandsData.brands);
        else document.getElementById('brands-container').innerHTML = '<div class="text-red-500 text-sm">Failed to load brands</div>';

        if (sizesData.status) renderSizes(sizesData.sizes);

        if (colorsData.status) renderColors(colorsData.colors);
        else document.getElementById('colors-container').innerHTML = '<div class="text-red-500 text-sm">Failed</div>';

    } catch (e) {
        console.error("Error loading filters", e);
        const errHtml = '<div class="text-red-500 text-sm">Error loading</div>';
        const bContainer = document.getElementById('brands-container');
        if (bContainer) bContainer.innerHTML = errHtml;
        const cContainer = document.getElementById('colors-container');
        if (cContainer) cContainer.innerHTML = errHtml;
    }
}

function renderBrands(brands) {
    const container = document.getElementById('brands-container');
    if (!container) return;

    container.innerHTML = brands.map(b => `
        <label class="flex items-center space-x-3 cursor-pointer group">
            <input type="checkbox" value="${b.id}"
                class="form-checkbox h-4 w-4 text-green-500 rounded border-gray-700 bg-transparent focus:ring-0 focus:ring-offset-0 brand-checkbox">
            <span class="text-gray-400 text-sm group-hover:text-white transition-colors">${b.name}</span>
        </label>
    `).join('');
}

function renderSizes(sizes) {
    const select = document.getElementById('size-select');
    if (!select) return;

    select.innerHTML = '<option value="" class="bg-black text-gray-500">Select Size</option>' +
        sizes.map(s => `<option value="${s.id}" class="bg-black">US ${s.name}</option>`).join('');

    // Removed auto listener
}

function renderColors(colors) {
    const container = document.getElementById('colors-container');
    if (!container) return;

    // Map backend color names to Tailwind CSS classes
    const colorMap = {
        'black': 'bg-black',
        'white': 'bg-white',
        'red': 'bg-red-600',
        'blue': 'bg-blue-600',
        'green': 'bg-green-500',
        'yellow': 'bg-yellow-400',
        'orange': 'bg-orange-500',
        'purple': 'bg-purple-600',
        'pink': 'bg-pink-500',
        'gray': 'bg-gray-500',
        'brown': 'bg-amber-800',
        'gold': 'bg-yellow-600',
        'silver': 'bg-gray-300',
        'navy': 'bg-blue-900',
        'beige': 'bg-[#F5F5DC]' // Custom hex for beige
    };

    container.innerHTML = colors.map(c => {
        const normalizedName = c.name.toLowerCase().trim();
        const bgClass = colorMap[normalizedName] || 'bg-gray-500';
        const needsBorder = ['white', 'silver', 'beige', 'yellow'].includes(normalizedName);
        const borderClass = needsBorder ? 'border-gray-700' : 'border-transparent';

        return `
            <button onclick="toggleColor(this, ${c.id})" title="${c.name}"
                class="w-8 h-8 rounded-full ${bgClass} border ${borderClass} hover:scale-110 transition-transform color-btn flex items-center justify-center"
                data-id="${c.id}">
                <i class="fas fa-check text-white text-xs hidden check-icon ${normalizedName === 'white' ? 'text-black' : ''}"></i>
            </button>
        `;
    }).join('');
}

function toggleColor(btn, id) {
    btn.classList.toggle('ring-2');
    btn.classList.toggle('ring-green-500');

    // Toggle check icon visibility
    const icon = btn.querySelector('.check-icon');
    if (icon) icon.classList.toggle('hidden');

    // Removed auto updateFilters call
}

function setupEventListeners() {
    // Search Input
    // Search Input
    // Auto-search removed. Query is read in updateFilters().
    // Optional: Allow Enter key to trigger search
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                updateFilters();
            }
        });
    }

    // Sort Select
    const sortSelect = document.getElementById('sort-select');
    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            currentState.sort = e.target.value;
            // Removed auto-search. User must click Apply.
        });
    }

    // Price Inputs
    setupPriceSlider();
}

function setupPriceSlider() {
    const minInput = document.getElementById('price-min');
    const maxInput = document.getElementById('price-max');
    const minVal = document.getElementById('price-min-val');
    const maxVal = document.getElementById('price-max-val');
    const track = document.getElementById('price-track');

    if (!minInput || !maxInput) return;

    let debounceTimer;

    function updateSlider() {
        let min = parseInt(minInput.value);
        let max = parseInt(maxInput.value);

        // Prevent crossing
        if (min > max - 500) {
            const tmp = min;
            min = max - 500;
            minInput.value = min;
        }

        if (max < min + 500) {
            max = min + 500;
            maxInput.value = max;
        }

        if (minVal) minVal.innerText = min;
        if (maxVal) maxVal.innerText = max;

        // Update Track
        // Assuming max possible value is 50000 based on HTML
        const maxTotal = 50000;
        const leftPercent = (min / maxTotal) * 100;
        const rightPercent = 100 - (max / maxTotal) * 100;

        if (track) {
            track.style.left = leftPercent + "%";
            track.style.right = rightPercent + "%";
        }

        // Removed auto-search. User must click Apply.
    }

    minInput.addEventListener('input', updateSlider);
    maxInput.addEventListener('input', updateSlider);

    // Init
    updateSlider();
}

function updateFilters() {
    // Brands
    const brandChecks = document.querySelectorAll('.brand-checkbox:checked');
    currentState.brandIds = Array.from(brandChecks).map(c => parseInt(c.value));

    // Size
    const sizeSelect = document.getElementById('size-select');
    currentState.sizeIds = sizeSelect && sizeSelect.value ? [parseInt(sizeSelect.value)] : [];

    // Colors
    const colorBtns = document.querySelectorAll('.color-btn.ring-2'); // My toggle class
    currentState.colorIds = Array.from(colorBtns).map(btn => parseInt(btn.dataset.id));

    // Price
    const minInput = document.getElementById('price-min');
    const maxInput = document.getElementById('price-max');
    if (minInput && maxInput) {
        currentState.minPrice = parseInt(minInput.value);
        currentState.maxPrice = parseInt(maxInput.value);
    }

    // Query
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        currentState.query = searchInput.value.trim();
    }

    currentState.page = 1;
    searchProducts();
}

async function searchProducts() {
    const grid = document.getElementById('product-grid');
    if (!grid) return;

    grid.innerHTML = '<div class="col-span-full text-center text-white py-10"><i class="fas fa-spinner fa-spin fa-2x"></i></div>';

    try {
        const response = await fetch(`${API_BASE_URL}/products/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(currentState)
        });

        const data = await response.json();

        if (data.status) {
            renderProducts(data.products);
            updateResultCount(data.products.length); // Should use totalResults from BE if available
        } else {
            grid.innerHTML = `<div class="col-span-full text-center text-red-500 py-10">${data.message}</div>`;
        }
    } catch (e) {
        console.error("Search error", e);
        grid.innerHTML = '<div class="col-span-full text-center text-red-500 py-10">Error loading products</div>';
    }
}

// Auth State
let currentUserSearch = null;
let userWishlistIds = new Set();

async function fetchWishlistIds() {
    if (!currentUserSearch) return;
    try {
        const response = await fetch('api/wishlist/ids');
        const data = await response.json();
        if (data.status) {
            userWishlistIds = new Set(data.ids);
        }
    } catch (e) {
        console.error("Error fetching wishlist IDs", e);
    }
}

// Call checkAuth on init
(async () => {
    try {
        const r = await fetch('api/auth/status');
        const d = await r.json();
        if (d.isLoggedIn) {
            currentUserSearch = { id: d.userId };
            await fetchWishlistIds();
            // Re-render if products already loaded? 
            // Usually products load after filters, which might be concurrent. 
            // Ideally we re-render or SEARCH triggers render.
        }
    } catch (e) { }
})();

function renderProducts(products) {
    const grid = document.getElementById('product-grid');
    if (!grid) return;

    if (products.length === 0) {
        grid.innerHTML = '<div class="col-span-full text-center text-gray-500 py-10">No products found matching your criteria.</div>';
        return;
    }

    grid.innerHTML = products.map(p => {
        const isInWishlist = userWishlistIds.has(p.id);
        const heartClass = isInWishlist ? "fas fa-heart text-green-500" : "far fa-heart";

        const wishlistBtn = currentUserSearch
            ? `<button onclick="event.stopPropagation(); addToWishlistSearch(${p.id}, this)" class="w-8 h-8 rounded-full ${isInWishlist ? 'bg-white' : 'bg-black/50'} backdrop-blur ${isInWishlist ? 'text-green-500' : 'text-white'} flex items-center justify-center hover:bg-green-500 hover:text-white transition-colors shadow-lg"><i class="${heartClass}"></i></button>`
            : '';

        return `
        <div class="group cursor-pointer relative bg-white/[0.03] rounded-2xl overflow-hidden border border-white/5 hover:border-green-500/30 hover:shadow-[0_8px_40px_rgba(34,197,94,0.15)] transition-all duration-500 hover:-translate-y-2"
             onclick="window.location.href='product.html?id=${p.id}'">
            <div class="absolute top-3 right-3 z-10">
                ${wishlistBtn}
            </div>
            <div class="w-full aspect-[4/5] flex items-center justify-center bg-gradient-to-b from-white/[0.02] to-transparent p-6 overflow-hidden">
                <img src="${p.imagePath ? p.imagePath : 'assets/images/placeholder.png'}"
                     class="max-w-full max-h-full object-contain rounded-2xl group-hover:scale-110 transition-transform duration-700 drop-shadow-2xl" 
                     alt="${p.name}">
            </div>
            <div class="p-5 border-t border-white/5">
                <p class="text-[10px] font-semibold text-gray-500 uppercase tracking-widest mb-1">${p.brandName || ''}</p>
                <h4 class="font-bold text-base text-white truncate mb-1" title="${p.name}">${p.name}</h4>
                <p class="text-xs text-gray-500 mb-3">${p.gender || ''}</p>
                <div class="flex items-center justify-between">
                    <span class="text-green-400 font-bold text-lg">Rs. ${p.price.toFixed(2)}</span>
                    <button onclick="event.stopPropagation(); addToCartSearch(${p.id}, this)"
                        class="bg-green-600 hover:bg-green-500 text-white text-xs font-bold px-5 py-2.5 rounded-xl transition-all duration-300 hover:shadow-[0_4px_15px_rgba(34,197,94,0.4)]">
                        ADD TO CART
                    </button>
                </div>
            </div>
        </div>
    `}).join('');
}

function addToCartSearch(productId, btn) {
    const originalText = btn.innerText;
    btn.innerText = 'Adding...';
    btn.disabled = true;

    fetch('api/cart/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            productId: productId,
            qty: 1
        })
    })
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                Notiflix.Notify.success("Added to Cart!");
            } else {
                Notiflix.Notify.failure(data.message || "Failed to add");
            }
        })
        .catch(e => {
            console.error(e);
            Notiflix.Notify.failure("Error");
        })
        .finally(() => {
            btn.innerText = originalText;
            btn.disabled = false;
        });
}

async function addToWishlistSearch(productId, btn) {
    if (!currentUserSearch) {
        Notiflix.Notify.info("Please login to add to wishlist");
        return;
    }

    const isIn = userWishlistIds.has(productId);
    const endpoint = isIn ? 'api/wishlist/remove' : 'api/wishlist/add';

    // Optimistic Update
    const icon = btn.querySelector('i');
    if (isIn) {
        userWishlistIds.delete(productId);
        icon.className = "far fa-heart";
        btn.classList.remove('bg-white', 'text-green-500');
        btn.classList.add('bg-black/50', 'text-white');
    } else {
        userWishlistIds.add(productId);
        icon.className = "fas fa-heart text-green-500";
        btn.classList.add('bg-white', 'text-green-500');
        btn.classList.remove('bg-black/50', 'text-white');
    }

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: productId })
        });
        const data = await response.json();
        if (data.status) {
            Notiflix.Notify.success(data.message);
            if (window.loadHeaderWishlistItems) window.loadHeaderWishlistItems();
        } else {
            Notiflix.Notify.info(data.message);
        }
    } catch (e) {
        console.error(e);
        Notiflix.Notify.failure("Error adding to wishlist");
    }
}

function updateResultCount(count) {
    const el = document.getElementById('result-count');
    if (el) el.innerText = count;
}
