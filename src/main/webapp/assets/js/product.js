const API_BASE_URL = 'api';



let currentUser = null;

async function checkAuth() {
    try {
        const response = await fetch('api/auth/status');
        const data = await response.json();
        if (data.isLoggedIn) {
            currentUser = { id: data.userId };
            // Show main wishlist button
            const mainBtn = document.getElementById('main-wishlist-btn');
            if (mainBtn) {
                mainBtn.style.display = 'flex';
                mainBtn.classList.remove('hidden');
            }
        } else {
            const mainBtn = document.getElementById('main-wishlist-btn');
            if (mainBtn) mainBtn.style.display = 'none';
        }
    } catch (e) {
        console.error("Auth check failed", e);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    await checkAuth(); // Run auth check first

    const urlParams = new URLSearchParams(window.location.search);
    const productId = urlParams.get('id');
    paramSizeId = urlParams.get('size');
    paramColorId = urlParams.get('color');
    const fromPage = urlParams.get('from');

    if (fromPage === 'cart') {
        const breadcrumb = document.querySelector('nav[aria-label="Breadcrumb"] ol li:nth-child(2) a');
        if (breadcrumb) {
            breadcrumb.href = 'cart.html';
            breadcrumb.innerText = 'Cart';
        }
    }

    if (productId) {
        loadProductDetails(productId);
    } else {
        document.querySelector('main').innerHTML = '<div class="text-center text-red-500 py-10">Product ID not found</div>';
    }
});

let currentProduct = null;
let currentQuantity = 1;
let selectedSizeId = null;
let selectedColorId = null;

// New Globals for URL params
let paramSizeId = null;
let paramColorId = null;

async function loadProductDetails(id) {
    Notiflix.Loading.standard('Loading Product Details...');
    try {
        // Fetch Product Details
        const response = await fetch(`${API_BASE_URL}/products/details?id=${id}`);

        if (!response.ok) {
            throw new Error(`Server returned ${response.status} ${response.statusText}`);
        }

        const data = await response.json();

        if (data.status) {
            currentProduct = data.product;
            availableCombinations = data.stockCombinations || [];
            renderProductInfo(data.product, data.images, data.sizes, data.colors);

            // Auto Select from URL Params
            if (paramColorId) {
                const btn = document.querySelector(`button[onclick*="selectColor(this, ${paramColorId})"]`);
                if (btn) btn.click();
            }
            if (paramSizeId) {
                const btn = document.querySelector(`button[onclick*="selectSize(this, ${paramSizeId})"]`);
                if (btn && !btn.disabled) btn.click();
            }

            loadRelatedProducts(data.product.brandId, data.product.id);
            loadReviews(data.product.id);
            checkWishlistStatus(data.product.id);
        } else {
            Notiflix.Notify.failure(data.message || "Product not found");
            document.querySelector('main').innerHTML = `<div class="text-center text-red-500 py-10 text-2xl font-bold">${data.message || "Product Not Found"}</div>`;
        }

    } catch (e) {
        console.error("Error loading product", e);
        Notiflix.Notify.failure("Something went wrong loading the product.");
        document.querySelector('main').innerHTML = `<div class="text-center text-red-500 py-10 text-xl">Something went wrong. Please try again later.</div>`;
    } finally {
        Notiflix.Loading.remove();
    }
}


let availableCombinations = []; // Map {sizeId, colorId}

function renderProductInfo(product, images, sizes, colors) {
    // Basic Info
    document.title = `Nike-X | ${product.name}`;
    safeSetText('product-name', product.name);
    safeSetText('product-price', `Rs. ${product.price.toFixed(2)}`);
    safeSetText('product-desc', product.description);
    safeSetText('breadcrumb-name', product.name);

    // Tag
    const tag = document.getElementById('product-tag');
    if (tag) {
        tag.innerText = 'In Stock';
        tag.className = `text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider mb-4 inline-block bg-green-600/20 text-green-500`;
    }

    // Process Images
    colorImages = {};
    const mainImg = document.getElementById('main-image');
    const thumbContainer = document.getElementById('thumbnail-container');

    let imageList = [];

    if (images && images.length > 0) {
        if (typeof images[0] === 'string') {
            imageList = images;
            if (mainImg) mainImg.src = images[0];
        } else {
            imageList = images.map(img => img.path);
            if (mainImg) mainImg.src = images[0].path;

            images.forEach(img => {
                if (img.colorId && !colorImages[img.colorId]) {
                    colorImages[img.colorId] = img.path;
                }
            });
        }
    } else if (mainImg) {
        mainImg.src = 'assets/images/placeholder.png';
    }

    if (thumbContainer && imageList.length > 0) {
        thumbContainer.innerHTML = imageList.map((img, index) => `
            <div onclick="changeMainImage('${img}')"
                class="glass-panel rounded-2xl p-4 aspect-square flex items-center justify-center cursor-pointer hover:border-green-500 transition-colors border border-transparent opacity-60 hover:opacity-100">
                <img src="${img}" class="w-full h-full object-contain rounded-xl">
            </div>
        `).join('');
    }

    // Sizes
    const sizeContainer = document.getElementById('size-container');
    if (sizeContainer) {
        if (!sizes || sizes.length === 0) {
            sizeContainer.innerHTML = '<span class="text-red-500 text-sm">Out of Stock</span>';
        } else {
            sizeContainer.innerHTML = sizes.map(s => `
                <button onclick="selectSize(this, ${s.id})" data-id="${s.id}"
                    class="h-10 rounded-lg border border-gray-700 hover:border-green-500 hover:bg-green-500/10 text-sm text-gray-400 hover:text-white transition-all size-btn">
                    ${s.name}
                </button>
            `).join('');
        }
    }

    // Colors
    const colorContainer = document.getElementById('color-container');
    if (colorContainer) {
        const colorMap = {
            'black': 'bg-black', 'white': 'bg-white', 'red': 'bg-red-600', 'blue': 'bg-blue-600',
            'green': 'bg-green-500', 'yellow': 'bg-yellow-400', 'orange': 'bg-orange-500',
            'purple': 'bg-purple-600', 'pink': 'bg-pink-500', 'gray': 'bg-gray-500',
            'brown': 'bg-amber-800', 'gold': 'bg-yellow-600', 'silver': 'bg-gray-300',
            'navy': 'bg-blue-900', 'beige': 'bg-[#F5F5DC]'
        };

        if (!colors || colors.length === 0) {
            colorContainer.innerHTML = '<span class="text-gray-500 text-sm">Standard</span>';
        } else {
            colorContainer.innerHTML = colors.map(c => {
                const normalized = c.name.toLowerCase().trim();
                const bgClass = colorMap[normalized] || 'bg-gray-500';
                const borderClass = ['white', 'silver', 'beige', 'yellow'].includes(normalized) ? 'border-gray-700' : 'border-transparent';

                return `
                <button onclick="selectColor(this, ${c.id})"
                    class="w-10 h-10 rounded-full ${bgClass} border-2 ${borderClass} hover:scale-110 transition-transform color-btn focus:outline-none flex items-center justify-center">
                     <i class="fas fa-check text-white text-xs hidden check-icon ${normalized === 'white' ? 'text-black' : ''}"></i>
                </button>
            `;
            }).join('');
        }
    }
}

function changeMainImage(src) {
    const img = document.getElementById('main-image');
    if (img) {
        img.style.opacity = '0';
        setTimeout(() => {
            img.src = src;
            img.style.opacity = '1';
        }, 300);
    }
}

function selectSize(btn, id) {
    if (btn.disabled) return;
    document.querySelectorAll('.size-btn').forEach(b => {
        b.classList.remove('bg-green-500', 'text-black', 'font-bold', 'border-green-500');
        b.classList.add('border-gray-700', 'text-gray-400');
    });
    btn.classList.remove('border-gray-700', 'text-gray-400');
    btn.classList.add('bg-green-500', 'text-black', 'font-bold', 'border-green-500');
    selectedSizeId = id;
    updatePrice();
}

function selectColor(btn, id) {
    document.querySelectorAll('.color-btn').forEach(b => {
        b.classList.remove('ring-2', 'ring-green-500/50', 'ring-offset-1', 'ring-offset-black');
        const icon = b.querySelector('.check-icon');
        if (icon) icon.classList.add('hidden');
    });
    btn.classList.add('ring-2', 'ring-green-500/50', 'ring-offset-1', 'ring-offset-black');
    const icon = btn.querySelector('.check-icon');
    if (icon) icon.classList.remove('hidden');
    selectedColorId = id;

    // Switch image if available
    if (colorImages[id]) {
        changeMainImage(colorImages[id]);
    }

    // Filter Sizes
    checkSizeAvailability(id);
    updatePrice();
}

function checkSizeAvailability(colorId) {
    const sizeBtns = document.querySelectorAll('.size-btn');
    let firstAvailable = null;

    sizeBtns.forEach(btn => {
        const sizeId = parseInt(btn.getAttribute('data-id'));
        // Check if combination exists in availableCombinations
        const exists = availableCombinations.some(c => c.colorId === colorId && c.sizeId === sizeId);

        if (exists) {
            btn.disabled = false;
            btn.classList.remove('opacity-20', 'cursor-not-allowed', 'bg-red-900/10');
            btn.classList.add('hover:border-green-500', 'hover:text-white', 'cursor-pointer');
            if (!firstAvailable) firstAvailable = btn;
        } else {
            btn.disabled = true;
            btn.classList.add('opacity-20', 'cursor-not-allowed', 'bg-red-900/10');
            btn.classList.remove('hover:border-green-500', 'hover:text-white', 'cursor-pointer', 'bg-green-500', 'text-black', 'font-bold');
            btn.classList.add('border-gray-700', 'text-gray-400'); // Reset style

            // If this was selected, deselect it
            if (selectedSizeId === sizeId) {
                selectedSizeId = null;
            }
        }
    });
}

function updatePrice() {
    console.log(`Updating Price: Color=${selectedColorId}, Size=${selectedSizeId}`);
    if (selectedColorId && selectedSizeId) {
        const combo = availableCombinations.find(c => c.colorId == selectedColorId && c.sizeId == selectedSizeId);
        console.log("Combo found:", combo);
        if (combo && combo.price) {
            safeSetText('product-price', `Rs. ${combo.price.toFixed(2)}`);
            return;
        }
    }
    // Default fallback
    if (currentProduct) {
        safeSetText('product-price', `Rs. ${currentProduct.price.toFixed(2)}`);
    }
}

function updateQty(change) {
    const input = document.getElementById('qty-input');
    if (!input) return;
    let val = parseInt(input.value) + change;
    if (val < 1) val = 1;
    if (val > 5) val = 5;
    input.value = val;
    currentQuantity = val;
}

function addToCart() {
    if (!selectedSizeId) {
        alert("Please select a size");
        return;
    }
    // Color check optional if colors exist
    const colorContainer = document.getElementById('color-container');
    const hasColors = colorContainer && colorContainer.children.length > 0 && !colorContainer.innerText.includes("Standard");
    if (hasColors && !selectedColorId) {
        alert("Please select a color");
        return;
    }

    // alert(`Adding to cart: Product ${currentProduct.id}, Size ${selectedSizeId}, Color ${selectedColorId}, Qty ${currentQuantity}`);

    const btn = document.querySelector('button[onclick="addToCart()"]');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Adding...';
    btn.disabled = true;

    const payload = {
        productId: currentProduct.id,
        sizeId: selectedSizeId,
        colorId: selectedColorId,
        qty: currentQuantity
    };

    fetch(`${API_BASE_URL}/cart/add`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                Notiflix.Notify.success("Added to Cart!");
                if (window.updateCartCount) window.updateCartCount();
            } else {
                Notiflix.Notify.failure(data.message || "Failed to add to cart");
            }
        })
        .catch(err => {
            console.error(err);
            Notiflix.Notify.failure("Error adding to cart");
        })
        .finally(() => {
            btn.innerHTML = originalText;
            btn.disabled = false;
        });
}

function buyNow() {
    if (!selectedSizeId) {
        alert("Please select a size");
        return;
    }
    // Color check optional if colors exist
    const colorContainer = document.getElementById('color-container');
    const hasColors = colorContainer && colorContainer.children.length > 0 && !colorContainer.innerText.includes("Standard");
    if (hasColors && !selectedColorId) {
        alert("Please select a color");
        return;
    }

    // Find Stock ID from combinations
    const combo = availableCombinations.find(c => c.sizeId == selectedSizeId && c.colorId == selectedColorId);

    if (!combo || !combo.stockId) {
        Notiflix.Notify.failure("Selected combination is unavailable.");
        return;
    }

    // Direct Redirect (No Cart Add)
    window.location.href = `checkout.html?stockId=${combo.stockId}&qty=${currentQuantity}`;
}

async function loadRelatedProducts(brandId, currentId) {
    try {
        const response = await fetch(`${API_BASE_URL}/products/related?brand_id=${brandId}&product_id=${currentId}`);
        const data = await response.json();

        const container = document.getElementById('related-products-container');
        if (!container || !data.status) return;

        if (!container || !data.status) return;

        container.innerHTML = data.products.map(p => {
            const wishlistBtn = currentUser
                ? `<button onclick="event.stopPropagation(); addToWishlistMain(${p.id})" class="w-8 h-8 rounded-full bg-black/50 backdrop-blur text-white flex items-center justify-center hover:bg-red-500 hover:text-white transition-colors"><i class="far fa-heart"></i></button>`
                : '';

            const imagePath = p.imagePath || 'assets/images/products/placeholder.png';

            return `
            <div class="glass-panel rounded-3xl overflow-hidden group cursor-pointer relative" onclick="window.location.href='product.html?id=${p.id}'">
                <div class="absolute top-4 left-4 bg-green-600 text-[10px] font-bold px-2 py-1 rounded text-white uppercase">
                    New
                </div>
                <div class="absolute top-4 right-4 z-10">
                    ${wishlistBtn}
                </div>
                <div class="h-64 flex items-center justify-center bg-white/5 group-hover:bg-white/10 transition-colors overflow-hidden">
                    <img src="${imagePath}" 
                        class="w-48 h-48 object-contain rounded-3xl group-hover:scale-110 transition-transform duration-500 drop-shadow-2xl grayscale group-hover:grayscale-0" 
                        alt="${p.name}">
                </div>
                <div class="p-6">
                    <div class="flex justify-between items-start mb-2">
                        <div>
                            <h4 class="font-bold text-lg text-white truncate w-32" title="${p.name}">${p.name}</h4>
                            <p class="text-xs text-gray-500">${p.modelName || ''}</p>
                        </div>
                        <span class="text-green-400 font-bold text-sm">Rs. ${p.price.toFixed(2)}</span>
                    </div>
                    <button onclick="event.stopPropagation(); addToCartQuick(${p.id})"
                        class="w-full mt-4 glass-morphism py-2 rounded-xl text-sm font-semibold text-white hover:bg-green-600 hover:border-green-600 transition-all">
                        Add to Cart
                    </button>
                </div>
            </div>
            `;
        }).join('');

    } catch (e) {
        console.error("Related products error", e);
    }
}

function addToCartQuick(productId) {
    const btn = event.currentTarget; // Get button
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
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
                if (window.updateCartCount) window.updateCartCount();
            } else {
                Notiflix.Notify.failure(data.message || "Failed to add");
            }
        })
        .catch(e => {
            console.error(e);
            Notiflix.Notify.failure("Error");
        })
        .finally(() => {
            btn.innerHTML = originalText;
            btn.disabled = false;
        });
}

function safeSetText(id, text) {
    const el = document.getElementById(id);
    if (el) el.innerText = text;
}

// --- Review Logic ---

async function loadReviews(productId) {
    try {
        const response = await fetch(`${API_BASE_URL}/products/reviews?product_id=${productId}`);
        const data = await response.json();
        const container = document.getElementById('reviews-list-container');
        const summaryContainer = document.getElementById('rating-summary-container');

        if (data.status && data.reviews.length > 0) {
            renderReviews(data.reviews, container);
            renderRatingSummary(data.reviews, summaryContainer);
        } else {
            container.innerHTML = '<div class="text-center text-gray-500 py-10">No reviews yet. Be the first to review!</div>';
            renderRatingSummary([], summaryContainer);
        }

        checkReviewEligibility(productId);

    } catch (e) {
        console.error("Error loading reviews", e);
    }
}

function renderReviews(reviews, container) {
    container.innerHTML = reviews.map(r => `
        <div class="glass-panel rounded-3xl p-6 mb-4">
            <div class="flex justify-between items-start mb-4">
                <div class="flex items-center gap-4">
                    <div class="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center text-sm font-bold border border-gray-600">
                        ${getInitials(r.userName)}
                    </div>
                    <div>
                        <h4 class="font-bold text-sm text-white">${r.userName}</h4>
                        <div class="flex items-center gap-2">
                            <div class="flex text-yellow-400 text-xs">
                                ${generateStars(r.rating)}
                            </div>
                            <span class="text-xs text-gray-500 bg-black/30 px-2 py-0.5 rounded-full">${r.variant}</span>
                        </div>
                    </div>
                </div>
                <span class="text-gray-500 text-xs">${r.date}</span>
            </div>
            <p class="text-gray-300 text-sm leading-relaxed mb-4">
                ${r.review}
            </p>
            ${renderReviewImages(r.images)}
        </div>
    `).join('');
}

function renderReviewImages(images) {
    if (!images || images.length === 0) return '';
    return `
        <div class="flex gap-2 mt-3 overflow-x-auto pb-2">
            ${images.map(img => `
                <div class="w-20 h-20 rounded-lg overflow-hidden border border-gray-700 shrink-0">
                    <img src="${img}" class="w-full h-full object-cover">
                </div>
            `).join('')}
        </div>
    `;
}

function renderRatingSummary(reviews, container) {
    const total = reviews.length;
    const avg = total > 0 ? (reviews.reduce((sum, r) => sum + r.rating, 0) / total).toFixed(1) : "0.0";

    const counts = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    reviews.forEach(r => {
        const rounded = Math.round(r.rating);
        if (counts[rounded] !== undefined) counts[rounded]++;
    });

    const bars = [5, 4, 3, 2, 1].map(stars => {
        const count = counts[stars];
        const pct = total > 0 ? (count / total * 100).toFixed(0) : 0;

        return `
            <div class="flex items-center gap-3 text-sm">
                <span class="w-8 text-gray-400 flex items-center gap-1">${stars} <i class="fas fa-star text-[10px]"></i></span>
                <div class="flex-1 h-2 bg-gray-700 rounded-full overflow-hidden">
                    <div class="h-full bg-brand-accent transition-all duration-500" style="width: ${pct}%"></div>
                </div>
                <span class="w-8 text-right text-gray-400">${pct}%</span>
            </div>
        `;
    }).join('');

    container.innerHTML = `
        <div class="text-center mb-8">
            <div class="text-6xl font-bold text-white mb-2">${avg}</div>
            <div class="flex justify-center text-yellow-400 text-sm mb-2">
                 ${generateStars(parseFloat(avg))}
            </div>
            <p class="text-gray-400 text-sm">Based on ${total} reviews</p>
        </div>
        <div class="space-y-3 mb-8">
            ${bars}
        </div>
        <button id="write-review-btn" onclick="openReviewModal()" disabled
            class="w-full bg-white/10 text-gray-500 py-3 rounded-xl font-medium transition-colors cursor-not-allowed">
            Loading eligibility...
        </button>
        <p id="eligibility-msg" class="text-xs text-center text-gray-500 mt-2 hidden"></p>
    `;

    // Update Header Stars
    const headerStars = document.getElementById('header-rating-stars');
    if (headerStars) {
        headerStars.innerHTML = `
            ${generateStars(parseFloat(avg))}
            <span class="text-gray-400 ml-2" id="review-count-display">(${total} Reviews)</span>
        `;
    }
}

async function checkReviewEligibility(productId) {
    const btn = document.getElementById('write-review-btn');
    if (!btn) return;

    try {
        const response = await fetch(`${API_BASE_URL}/products/reviews/check-eligibility?product_id=${productId}`);
        const data = await response.json();

        if (data.status) {
            btn.disabled = false;
            btn.classList.remove('bg-white/10', 'text-gray-500', 'cursor-not-allowed');
            btn.classList.add('bg-brand-accent', 'hover:bg-green-600', 'text-white');
            btn.innerText = 'Write a Review';
        } else {
            btn.innerText = 'Verified Owners Only';
            const msg = document.getElementById('eligibility-msg');
            if (msg) {
                msg.innerText = "You must purchase this verified product to leave a review.";
                msg.classList.remove('hidden');
            }
        }
    } catch (e) {
        btn.innerText = 'Login to Review';
        btn.onclick = () => window.location.href = 'sign-in.html';
        btn.disabled = false;
        btn.classList.remove('cursor-not-allowed');
    }
}

function getInitials(name) {
    return name ? name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : '??';
}

function generateStars(rating) {
    let stars = '';
    for (let i = 1; i <= 5; i++) {
        if (rating >= i) stars += '<i class="fas fa-star"></i>';
        else if (rating >= i - 0.5) stars += '<i class="fas fa-star-half-alt"></i>';
        else stars += '<i class="far fa-star text-gray-600"></i>';
    }
    return stars;
}

// --- Modal & Form Logic ---

let currentRating = 0;
let reviewImagesBase64 = [];

function openReviewModal() {
    document.getElementById('review-modal').classList.remove('hidden');
}

function closeReviewModal() {
    document.getElementById('review-modal').classList.add('hidden');
    document.getElementById('review-form').reset();
    currentRating = 0;
    updateStarInputGUI();
    reviewImagesBase64 = [];
    document.getElementById('image-preview-container').innerHTML = '';
}

function setRating(val) {
    currentRating = val;
    document.getElementById('rating-value').value = val;
    updateStarInputGUI();
}

function updateStarInputGUI() {
    const stars = document.getElementById('star-rating-input').children;
    for (let i = 0; i < stars.length; i++) {
        if (i < currentRating) {
            stars[i].classList.add('text-yellow-400');
            stars[i].classList.remove('text-gray-600');
        } else {
            stars[i].classList.remove('text-yellow-400');
            stars[i].classList.add('text-gray-600');
        }
    }
}

function previewImages(input) {
    const container = document.getElementById('image-preview-container');
    container.innerHTML = '';
    reviewImagesBase64 = [];

    if (input.files) {
        Array.from(input.files).forEach(file => {
            const reader = new FileReader();
            reader.onload = (e) => {
                reviewImagesBase64.push(e.target.result);

                const div = document.createElement('div');
                div.className = 'w-16 h-16 rounded overflow-hidden border border-gray-600 shrink-0';
                div.innerHTML = `<img src="${e.target.result}" class="w-full h-full object-cover">`;
                container.appendChild(div);
            };
            reader.readAsDataURL(file);
        });
    }
}

async function submitReview(e) {
    e.preventDefault();
    if (currentRating === 0) {
        alert("Please select a star rating");
        return;
    }

    const text = document.getElementById('review-text').value;

    const payload = {
        productId: currentProduct.id,
        review: text,
        rating: currentRating,
        images: reviewImagesBase64
    };

    const btn = e.target.querySelector('button[type="submit"]');
    const originalText = btn.innerText;
    btn.innerText = 'Submitting...';
    btn.disabled = true;

    try {
        const response = await fetch(`${API_BASE_URL}/products/reviews`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (data.status) {
            alert("Review submitted successfully!");
            closeReviewModal();
            loadReviews(currentProduct.id);
        } else {
            alert(data.message || "Failed to submit review");
        }

    } catch (err) {
        console.error(err);
        alert("Error submitting review");
    } finally {
        btn.innerText = originalText;
        btn.disabled = false;
    }
}

// --- Wishlist Logic ---

let isInWishlist = false;

async function checkWishlistStatus(productId) {
    if (!currentUser) return;

    try {
        const response = await fetch(`api/wishlist/check/${productId}`);
        const data = await response.json();

        if (data.status) {
            isInWishlist = true;
            updateWishlistBtnState(true);
        } else {
            isInWishlist = false;
            updateWishlistBtnState(false);
        }
    } catch (e) {
        console.error("Error checking wishlist", e);
    }
}

function updateWishlistBtnState(active) {
    const btn = document.getElementById('main-wishlist-btn');
    if (!btn) return;

    if (active) {
        btn.classList.add('bg-green-500', 'text-black');
        btn.classList.remove('bg-black/40', 'text-white', 'hover:bg-green-500', 'hover:text-black');
        // Add hover effect to indicate "Remove" logic if desired, or just keep it active status
        btn.title = "Remove from Wishlist";
    } else {
        btn.classList.remove('bg-green-500', 'text-black');
        btn.classList.add('bg-black/40', 'text-white', 'hover:bg-green-500', 'hover:text-black');
        btn.title = "Add to Wishlist";
    }
}

async function addToWishlistMain(id) {
    if (!currentUser) {
        Notiflix.Notify.info("Please login to add to wishlist");
        return;
    }

    const productId = id || (currentProduct ? currentProduct.id : null);
    if (!productId) return;

    // Toggle Logic
    const endpoint = isInWishlist ? 'api/wishlist/remove' : 'api/wishlist/add';
    const method = 'POST';

    const btn = document.getElementById('main-wishlist-btn');
    // Optimistic UI update
    const prevState = isInWishlist;
    isInWishlist = !isInWishlist;
    updateWishlistBtnState(isInWishlist);

    try {
        const response = await fetch(endpoint, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: productId })
        });
        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success(data.message);
            // Refresh Header Wishlist
            if (window.loadHeaderWishlistItems) {
                window.loadHeaderWishlistItems();
            }
        } else {
            // Revert on failure
            isInWishlist = prevState;
            updateWishlistBtnState(isInWishlist);
            Notiflix.Notify.info(data.message);
        }
    } catch (e) {
        console.error(e);
        isInWishlist = prevState;
        updateWishlistBtnState(isInWishlist);
        Notiflix.Notify.failure("Error updating wishlist");
    }
}
