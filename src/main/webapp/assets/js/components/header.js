function loadHeader(basePath = '') {
    if (basePath && !basePath.endsWith('/')) basePath += '/';
    window.appBasePath = basePath;
    const headerHTML = `
    <header class="fixed w-full top-0 z-50 transition-all duration-300">
        <div class="container mx-auto px-6 py-4">
            <nav class="glass-panel rounded-2xl px-6 py-3 flex justify-between items-center">
                <!-- Logo -->
                <a href="${basePath}index.html" class="text-2xl font-bold tracking-tighter hover:text-green-400 transition-colors">
                    NIKE<span class="text-green-500">-X</span>
                </a>

                <!-- Main Navigation -->
                <div class="hidden md:flex space-x-8 text-sm font-medium text-gray-300">
                    <a href="${basePath}index.html#home" class="hover:text-white transition-colors relative group">
                        Home
                        <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-green-500 transition-all group-hover:w-full"></span>
                    </a>
                    <a href="${basePath}index.html#about" class="hover:text-white transition-colors relative group">
                        About Us
                        <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-green-500 transition-all group-hover:w-full"></span>
                    </a>
                    <a href="${basePath}advanced-search.html" class="hover:text-white transition-colors relative group">
                        Products
                        <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-green-500 transition-all group-hover:w-full"></span>
                    </a>
                </div>

                <!-- Right Actions -->
                <div class="flex items-center space-x-6">
                    <div class="hidden lg:flex items-center space-x-4 border-r border-white/10 pr-6 relative">
                        <button onclick="toggleSearch()" class="text-gray-400 hover:text-white transition-colors"><i class="fas fa-search"></i></button>
                        
                        <!-- Wishlist Wrapper (Hidden by default) -->
                        <div class="relative hidden" id="header-wishlist-container">
                            <button id="header-wishlist-btn" onclick="toggleHeaderWishlist()" class="text-gray-400 hover:text-white transition-colors relative">
                                <i class="far fa-heart"></i>
                            </button>
                            <!-- Wishlist Dropdown -->
                            <div id="header-wishlist-dropdown" class="hidden absolute top-full right-0 mt-4 w-80 rounded-2xl z-50 animate-fade-in-down border border-white/10 shadow-2xl overflow-hidden" style="background: rgba(0, 0, 0, 0.7) !important; backdrop-filter: blur(20px) !important; -webkit-backdrop-filter: blur(20px) !important;">
                                <div class="px-4 py-3 border-b border-white/10 flex justify-between items-center">
                                    <h3 class="text-sm font-bold text-white">Wishlist</h3>
                                    <a href="${basePath}user/wishlist.html" class="text-xs text-green-400 hover:text-green-300">View All</a>
                                </div>
                                <div id="header-wishlist-content" class="max-h-64 overflow-y-auto custom-scrollbar">
                                    <!-- Items or Loading -->
                                    <div class="p-4 text-center text-gray-500 text-xs">Loading...</div>
                                </div>
                            </div>
                        </div>

                        <button onclick="window.location.href='${basePath}cart.html'" class="text-gray-400 hover:text-white transition-colors relative">
                            <i class="fas fa-shopping-cart"></i>
                            <span id="cart-badge-desktop" class="absolute -top-2 -right-2 bg-brand-accent text-black text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center hidden">0</span>
                        </button>
                    </div>

                    <!-- User Auth Container -->
                    <div class="relative group hidden sm:block" id="auth-container">
                         <!-- Dynamically populated by auth-check in index.js or dedicated shared auth script -->
                         <a href="${basePath}sign-in.html"
                            class="glass-morphism px-5 py-2 rounded-full text-sm font-semibold hover:bg-white/10 transition-all flex items-center gap-2">
                            <i class="far fa-user"></i>
                            <span>Sign In</span>
                        </a>
                    </div>

                    <!-- Mobile Menu Button -->
                    <button id="mobile-menu-btn" class="md:hidden text-white text-2xl focus:outline-none">
                        <i class="fas fa-bars"></i>
                    </button>
                </div>
            </nav>

            <!-- Mobile Menu Overlay -->
            <div id="mobile-menu" class="hidden md:hidden absolute top-full left-0 w-full mt-4 px-6 z-40">
                <div class="glass-panel rounded-2xl p-6 flex flex-col space-y-4">
                    <a href="${basePath}index.html#home" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors">Home</a>
                    <a href="${basePath}index.html#about" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors">About Us</a>
                    <a href="${basePath}advanced-search.html" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors">Products</a>
                     <a href="#" onclick="toggleSearch(); return false;" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors flex items-center gap-2">
                        <i class="fas fa-search"></i> Search
                    </a>
                    <a href="${basePath}cart.html" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors flex items-center gap-2">
                        <div class="relative">
                            <i class="fas fa-shopping-cart"></i>
                            <span id="cart-badge-mobile" class="absolute -top-2 -right-2 bg-brand-accent text-black text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center hidden">0</span>
                        </div>
                        Cart
                    </a>
                    <hr class="border-white/10">
                    <div id="mobile-auth-container">
                        <a href="${basePath}sign-in.html" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors flex items-center gap-2">
                            <i class="far fa-user"></i> Sign In
                        </a>
                    </div>
                </div>
            </div>
        </div>
        </div>
    </header>

    <!-- Search Overlay -->
    <div id="search-overlay" class="fixed inset-0 z-50 hidden bg-black/90 backdrop-blur-xl flex flex-col items-center justify-center transition-opacity duration-300 opacity-0">
        <button onclick="toggleSearch()" class="absolute top-8 right-8 text-white/50 hover:text-white text-4xl focus:outline-none">
            <i class="fas fa-times"></i>
        </button>
        
        <div class="w-full max-w-4xl px-6 relative transform transition-all scale-95" id="search-container">
            <h2 class="text-white/50 text-sm font-bold uppercase tracking-widest mb-4 text-center">Search Nike-X</h2>
            <div class="relative group">
                <input type="text" id="global-search-input" 
                    class="w-full bg-transparent border-b-2 border-white/20 text-white text-4xl md:text-6xl font-black py-4 focus:outline-none focus:border-green-500 transition-colors placeholder-white/10 text-center"
                    placeholder="WHAT ARE YOU LOOKING FOR?">
                <button onclick="performGlobalSearch()" class="absolute right-0 top-1/2 -translate-y-1/2 text-white/20 group-hover:text-green-500 transition-colors text-3xl">
                    <i class="fas fa-arrow-right"></i>
                </button>
            </div>
            <p class="text-center text-white/30 mt-6 text-sm">Press Enter to search</p>
        </div>
    </div>
    `;

    document.getElementById('header-placeholder').innerHTML = headerHTML;

    // Initialize Mobile Menu Logic
    const menuBtn = document.getElementById('mobile-menu-btn');
    const mobileMenu = document.getElementById('mobile-menu');

    if (menuBtn && mobileMenu) {
        menuBtn.addEventListener('click', () => {
            mobileMenu.classList.toggle('hidden');
            const icon = menuBtn.querySelector('i');
            if (mobileMenu.classList.contains('hidden')) {
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            } else {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-times');
            }
        });
    }

    // Initialize Auth Check
    checkUserAuth(basePath);

    // Initialize Cart Count
    // Expose function globally so other scripts can call it
    window.updateCartCount = async () => {
        try {
            const response = await fetch(`${basePath}api/cart/count`);
            const data = await response.json();

            if (data.status) {
                const count = data.count;
                const badges = [document.getElementById('cart-badge-desktop'), document.getElementById('cart-badge-mobile')];

                badges.forEach(badge => {
                    if (badge) {
                        badge.innerText = count;
                        if (count > 0) {
                            badge.classList.remove('hidden');
                        } else {
                            badge.classList.add('hidden');
                        }
                    }
                });
            }
        } catch (e) {
            console.error("Failed to update cart count", e);
        }
    };

    window.updateCartCount();

    // Global Search Logic
    window.toggleSearch = function () {
        const overlay = document.getElementById('search-overlay');
        const container = document.getElementById('search-container');
        const input = document.getElementById('global-search-input');

        if (overlay.classList.contains('hidden')) {
            // Open
            overlay.classList.remove('hidden');
            // Small delay for transition
            setTimeout(() => {
                overlay.classList.remove('opacity-0');
                container.classList.remove('scale-95');
                container.classList.add('scale-100');
                input.focus();
            }, 10);
            document.body.style.overflow = 'hidden';
        } else {
            // Close
            overlay.classList.add('opacity-0');
            container.classList.remove('scale-100');
            container.classList.add('scale-95');
            setTimeout(() => {
                overlay.classList.add('hidden');
            }, 300); // Match duration-300
            document.body.style.overflow = '';
        }
    };

    window.performGlobalSearch = function () {
        const query = document.getElementById('global-search-input').value.trim();
        if (query) {
            window.location.href = `${window.appBasePath || ''}advanced-search.html?query=${encodeURIComponent(query)}`;
        }
    };

    const globalInput = document.getElementById('global-search-input');
    if (globalInput) {

        // Suggestion Container
        const suggestionBox = document.createElement('div');
        suggestionBox.id = 'search-suggestions';
        suggestionBox.className = 'absolute top-full left-0 w-full mt-2 bg-black/90 backdrop-blur-xl border border-white/10 rounded-xl overflow-hidden hidden';
        suggestionBox.style.zIndex = '9999';
        globalInput.parentElement.appendChild(suggestionBox);

        let debounceTimer;

        globalInput.addEventListener('input', (e) => {
            const query = e.target.value.trim();
            clearTimeout(debounceTimer);

            if (query.length < 2) {
                suggestionBox.classList.add('hidden');
                return;
            }

            debounceTimer = setTimeout(async () => {
                try {
                    const response = await fetch(`${window.appBasePath || ''}api/products/suggest?query=${encodeURIComponent(query)}`);
                    const data = await response.json();

                    if (data.status && data.suggestions && data.suggestions.length > 0) {
                        suggestionBox.innerHTML = data.suggestions.map(s => `
                            <div class="px-6 py-3 hover:bg-white/10 cursor-pointer text-white/80 hover:text-green-400 transition-colors border-b border-white/5 last:border-0 flex justify-between items-center group"
                                 onclick="window.location.href='${window.appBasePath || ''}product.html?id=${s.id}'">
                                <div class="flex items-center gap-3">
                                    <i class="fas fa-search text-white/20 text-xs"></i>
                                    <span class="font-medium">${s.name}</span>
                                    <span class="text-xs text-white/30 bg-white/5 px-2 py-0.5 rounded-full">${s.brand}</span>
                                </div>
                                <i class="fas fa-arrow-right opacity-0 group-hover:opacity-100 transition-opacity text-xs"></i>
                            </div>
                        `).join('');
                        suggestionBox.classList.remove('hidden');
                    } else if (data.status) {
                        suggestionBox.innerHTML = '<div class="px-6 py-4 text-white/30 text-center text-sm">No products found</div>';
                        suggestionBox.classList.remove('hidden');
                    } else {
                        suggestionBox.classList.add('hidden');
                    }
                } catch (err) {
                    console.error("Suggestion error", err);
                    suggestionBox.classList.add('hidden');
                }
            }, 300);
        });

        globalInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                window.performGlobalSearch();
            }
        });

        // Close on escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (!suggestionBox.classList.contains('hidden')) {
                    suggestionBox.classList.add('hidden');
                } else if (!document.getElementById('search-overlay').classList.contains('hidden')) {
                    window.toggleSearch();
                }
            }
        });

        // Close suggestions on click outside
        document.addEventListener('click', (e) => {
            if (!globalInput.contains(e.target) && !suggestionBox.contains(e.target)) {
                suggestionBox.classList.add('hidden');
            }
        });
    }
}



async function checkUserAuth(basePath) {
    try {
        const response = await fetch(`${basePath}api/users/auth-check`);
        const data = await response.json();

        // Desktop Container
        const authContainer = document.getElementById('auth-container');
        // Mobile Container
        const mobileAuthContainer = document.getElementById('mobile-auth-container');

        if (data.status) {
            // --- LOGGED IN ---
            const user = data.user;
            const initial = user.firstName.charAt(0).toUpperCase();

            // 1. Update Desktop Header
            if (authContainer) {
                authContainer.innerHTML = `
                    <div class="relative">
                        <button id="user-menu-btn" onclick="toggleUserMenu()" 
                            class="w-10 h-10 rounded-full bg-green-600 flex items-center justify-center text-white font-bold hover:bg-green-700 transition-colors shadow-lg shadow-green-600/30">
                            ${initial}
                        </button>
                        
                        <!-- Dropdown -->
                        <div id="user-dropdown" class="hidden absolute right-0 mt-4 w-48 glass-panel rounded-xl py-2 z-50 animate-fade-in-down border border-white/10 shadow-2xl bg-black/90 backdrop-blur-xl">
                            <div class="px-4 py-3 border-b border-white/10 mb-2">
                                <p class="text-sm text-white font-bold truncate">${user.firstName}</p>
                                <p class="text-xs text-gray-500 truncate">${user.email}</p>
                            </div>
                            
                            <a href="${basePath}user/profile.html" class="block px-4 py-2 text-sm text-gray-300 hover:bg-white/10 hover:text-green-400 transition-colors flex items-center gap-2">
                                <i class="far fa-user w-4"></i> Profile
                            </a>
                            <a href="${basePath}user/orders.html" class="block px-4 py-2 text-sm text-gray-300 hover:bg-white/10 hover:text-green-400 transition-colors flex items-center gap-2">
                                <i class="fas fa-box w-4"></i> Orders
                            </a>
                            <a href="${basePath}user/wishlist.html" class="block px-4 py-2 text-sm text-gray-300 hover:bg-white/10 hover:text-green-400 transition-colors flex items-center gap-2">
                                <i class="far fa-heart w-4"></i> Wishlist
                            </a>
                            <a href="${basePath}advanced-search.html" class="block px-4 py-2 text-sm text-gray-300 hover:bg-white/10 hover:text-green-400 transition-colors flex items-center gap-2">
                                <i class="fas fa-search w-4"></i> Advanced Search
                            </a>
                            
                            <div class="border-t border-white/10 mt-2 pt-2">
                                <button onclick="userLogout('${basePath}')" class="w-full text-left block px-4 py-2 text-sm text-red-400 hover:bg-white/10 transition-colors flex items-center gap-2">
                                    <i class="fas fa-sign-out-alt w-4"></i> Sign Out
                                </button>
                            </div>
                        </div>
                    </div>
                `;

                // Logic for User Menu Toggling (Desktop)
                // Remove old listeners to avoid duplicates if checked multiple times, but this runs once mostly.
                // We'll rely on the onclick inline for toggle. 
                // Outside click listener is added globally or below.
            }

            // 2. Perform Wishlist Icon Check (Desktop)
            const wishlistContainer = document.getElementById('header-wishlist-container');
            if (wishlistContainer) wishlistContainer.classList.remove('hidden');

            // 3. Update Mobile Menu
            if (mobileAuthContainer) {
                mobileAuthContainer.innerHTML = `
                    <div class="flex items-center gap-3 mb-4 px-2">
                        <div class="w-10 h-10 rounded-full bg-green-600 flex items-center justify-center text-white font-bold text-lg">
                            ${initial}
                        </div>
                        <div>
                            <p class="text-white font-bold">${user.firstName}</p>
                            <p class="text-xs text-gray-400">${user.email}</p>
                        </div>
                    </div>
                    <div class="space-y-2">
                         <a href="${basePath}user/wishlist.html" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors flex items-center gap-2 px-2 py-1">
                            <i class="far fa-heart w-6 text-center"></i> Wishlist
                        </a>
                        <a href="${basePath}user/profile.html" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors flex items-center gap-2 px-2 py-1">
                            <i class="far fa-user w-6 text-center"></i> Profile
                        </a>
                        <a href="${basePath}user/orders.html" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors flex items-center gap-2 px-2 py-1">
                            <i class="fas fa-box w-6 text-center"></i> Orders
                        </a>
                         <button onclick="userLogout('${basePath}')" class="w-full text-left text-lg font-medium text-red-400 hover:text-red-300 transition-colors flex items-center gap-2 px-2 py-1">
                            <i class="fas fa-sign-out-alt w-6 text-center"></i> Sign Out
                        </button>
                    </div>
                `;
            }

            // Close dropdown when clicking outside (Desktop)
            // Note: We should ensure this listener isn't added multiple times if checkUserAuth is called multiple times.
            // But usually it's called once on page load.
            if (!window.userMenuListenerAdded) {
                document.addEventListener('click', (e) => {
                    const btn = document.getElementById('user-menu-btn');
                    const dropdown = document.getElementById('user-dropdown');
                    if (btn && dropdown && !btn.contains(e.target) && !dropdown.contains(e.target)) {
                        dropdown.classList.add('hidden');
                    }
                });
                window.userMenuListenerAdded = true;
            }


        } else {
            // --- NOT LOGGED IN ---

            // 1. Hide Desktop Wishlist
            const wishlistContainer = document.getElementById('header-wishlist-container');
            if (wishlistContainer) wishlistContainer.classList.add('hidden');

            // 2. Desktop Auth Button
            if (authContainer) {
                authContainer.innerHTML = `
                    <a href="${basePath}sign-in.html"
                        class="glass-morphism px-5 py-2 rounded-full text-sm font-semibold hover:bg-white/10 transition-all flex items-center gap-2">
                        <i class="far fa-user"></i>
                        <span>Sign In</span>
                    </a>
                 `;
            }

            // 3. Mobile Auth Button
            if (mobileAuthContainer) {
                mobileAuthContainer.innerHTML = `
                    <a href="${basePath}sign-in.html" class="text-lg font-medium text-gray-300 hover:text-green-400 transition-colors flex items-center gap-2">
                        <i class="far fa-user"></i> Sign In
                    </a>
                `;
            }
        }

    } catch (error) {
        console.error('Auth check failed:', error);
    }
}

function toggleUserMenu() {
    const dropdown = document.getElementById('user-dropdown');
    if (dropdown) {
        dropdown.classList.toggle('hidden');
    }
}

async function userLogout(basePath) {
    try {
        const response = await fetch(`${basePath}api/users/logout`);
        const data = await response.json();
        if (data.status) {
            window.location.reload();
        } else {
            alert("Logout failed");
        }
    } catch (e) {
        console.error("Logout error", e);
    }
}

// Wishlist Logic for Header
let isWishlistOpen = false;

function toggleHeaderWishlist() {
    const dropdown = document.getElementById('header-wishlist-dropdown');
    if (!dropdown) return;

    isWishlistOpen = !isWishlistOpen;
    if (isWishlistOpen) {
        dropdown.classList.remove('hidden');
        loadHeaderWishlistItems();
    } else {
        dropdown.classList.add('hidden');
    }
}

// Close when clicking outside
document.addEventListener('click', (e) => {
    const btn = document.getElementById('header-wishlist-btn');
    const dropdown = document.getElementById('header-wishlist-dropdown');
    if (isWishlistOpen && btn && dropdown && !btn.contains(e.target) && !dropdown.contains(e.target)) {
        isWishlistOpen = false;
        dropdown.classList.add('hidden');
    }
});

async function loadHeaderWishlistItems() {
    const container = document.getElementById('header-wishlist-content');
    if (!container) return;

    container.innerHTML = '<div class="p-4 text-center"><i class="fas fa-spinner fa-spin text-green-500"></i></div>';

    try {
        // Need to know base path? Ideally pass it or assume relative if on same domain. 
        // header.js functions are global, but inside loadHeader basePath is local.
        // We can infer it or rely on relative 'api/' if we are careful. 
        // Best hack: Check if we have a global basePath variable, or try relative.
        // Since all pages use <base> or relative includes, 'api/wishlist' should work from root.
        // But if we are in /user/orders.html, we need ../api/wishlist.
        // Simplest fix: Use window.location.pathname to determine depth or try to use the basePath stored/passed.
        // Actually, header.js is included. Let's try native fetch to 'api/wishlist' and rely on browser or <base> tag if exists (user doesn't use base tag).
        // A safer bet is to use the `basePath` from `loadHeader`. I will store it globally.

        const path = (window.appBasePath || '') + 'api/wishlist';

        const response = await fetch(path);
        const data = await response.json();

        if (data.status === false && data.message && data.message.includes("login")) {
            container.innerHTML = `
                <div class="p-4 text-center">
                    <p class="text-gray-400 text-xs mb-2">Please login to view wishlist</p>
                    <a href="${window.appBasePath || ''}sign-in.html" class="text-green-400 text-xs font-bold hover:underline">Sign In</a>
                </div>
             `;
            return;
        }

        if (data.status) {
            if (!data.items || data.items.length === 0) {
                container.innerHTML = '<div class="p-4 text-center text-gray-500 text-xs">Your wishlist is empty.</div>';
                return;
            }

            container.innerHTML = data.items.map(item => `
                <div class="flex items-center gap-3 p-3 hover:bg-white/5 transition-colors border-b border-white/5 last:border-0">
                    <img src="${item.imagePath ? item.imagePath : (window.appBasePath || '') + 'assets/images/placeholder.png'}" 
                         class="w-12 h-12 object-contain rounded-lg bg-white/5 cursor-pointer"
                         onclick="window.location.href='${window.appBasePath || ''}product.html?id=${item.id}'">
                    
                    <div class="flex-1 min-w-0 cursor-pointer" onclick="window.location.href='${window.appBasePath || ''}product.html?id=${item.id}'">
                        <h4 class="text-white text-sm font-medium truncate">${item.name}</h4>
                        <p class="text-xs text-gray-500 truncate">${item.brand}</p>
                        <p class="text-green-400 text-xs font-bold mt-1">Rs. ${item.price.toFixed(2)}</p>
                    </div>
                    
                    <button onclick="removeFromHeaderWishlist(${item.id}, this)" 
                            class="text-gray-500 hover:text-red-500 transition-colors p-2" title="Remove">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            `).join('');

        } else {
            container.innerHTML = '<div class="p-4 text-center text-red-400 text-xs">Failed to load.</div>';
        }
    } catch (e) {
        console.error(e);
        container.innerHTML = '<div class="p-4 text-center text-red-400 text-xs">Error loading.</div>';
    }
}

async function removeFromHeaderWishlist(id, btn) {
    // Optimistic UI or loading
    const icon = btn.querySelector('i');
    icon.className = "fas fa-spinner fa-spin";

    try {
        const path = (window.appBasePath || '') + 'api/wishlist/remove';
        const response = await fetch(path, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: id })
        });
        const data = await response.json();

        if (data.status) {
            // Reload list
            loadHeaderWishlistItems();
            Notiflix.Notify.success("Removed from wishlist");
        } else {
            Notiflix.Notify.failure(data.message);
            icon.className = "fas fa-trash-alt";
        }
    } catch (e) {
        console.error(e);
        Notiflix.Notify.failure("Error removing");
        icon.className = "fas fa-trash-alt";
    }
}
