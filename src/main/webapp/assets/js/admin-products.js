let productImages = [];

// Pagination State
let currentPage = 1;

// Modal Logic
document.addEventListener('DOMContentLoaded', () => {
    loadProducts(currentPage);
});

function openAddProductModal() {
    const modal = document.getElementById('add-product-modal');
    const content = document.getElementById('add-product-modal-content');
    modal.classList.remove('hidden');

    // Load brands when modal opens
    loadBrands();

    // Small delay to allow display:block to apply before transform
    setTimeout(() => {
        content.classList.remove('translate-x-full');
    }, 10);
}

async function loadProducts(page = 1) {
    currentPage = page;
    const tableBody = document.getElementById('product-table-body');
    // Clear table only if first load or explicit refresh, to reduce flicker, but for now simple innerHTML reset
    tableBody.innerHTML = '<tr><td colspan="7" class="px-6 py-4 text-center text-zinc-500">Loading products...</td></tr>';

    try {
        const response = await fetch(`../api/admin/product/list?page=${page}`);
        const data = await response.json();

        if (data.status) {
            tableBody.innerHTML = '';

            if (data.products.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="7" class="px-6 py-4 text-center text-zinc-500">No products found.</td></tr>';
                renderPagination(0, page);
                return;
            }

            data.products.forEach(product => {
                const isActive = product.status && product.status.toLowerCase() === 'active';
                const statusColor = isActive ? 'emerald' : 'red';
                const statusText = isActive ? 'ACTIVE' : 'INACTIVE';
                const imagePath = product.imagePath ? `../${product.imagePath}` : '../../assets/images/bg.png';

                const row = `
                    <tr class="hover:bg-white/5 transition-colors group cursor-pointer" onclick="openProductModal(${product.id})">
                        <td class="px-6 py-4 flex items-center gap-4">
                            <div class="w-12 h-12 rounded bg-zinc-800 flex items-center justify-center overflow-hidden border border-white/10">
                                <img src="${imagePath}" class="w-full h-full object-cover opacity-80 group-hover:scale-110 transition-transform duration-500">
                            </div>
                            <div>
                                <p class="font-medium text-white">${product.name}</p>
                                <p class="text-xs text-zinc-500">ID: ${product.id}</p>
                            </div>
                        </td>
                        <td class="px-6 py-4">${product.brandName}</td>
                        <td class="px-6 py-4">${product.modelName}</td>
                        <td class="px-6 py-4">
                            <div class="flex items-center gap-2">
                                <div class="w-2 h-2 rounded-full bg-${statusColor}-500"></div>
                                <span class="text-white">${product.stockQty}</span>
                            </div>
                        </td>
                        <td class="px-6 py-4">
                            <span class="px-2 py-1 rounded text-[10px] font-bold bg-${statusColor}-500/10 text-${statusColor}-500 border border-${statusColor}-500/20">
                                ${statusText}
                            </span>
                        </td>
                        <td class="px-6 py-4 text-right">
                            <button class="text-zinc-400 hover:text-white p-2">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                </svg>
                            </button>
                        </td>
                    </tr>
                `;
                tableBody.insertAdjacentHTML('beforeend', row);
            });

            renderPagination(data.totalPages, data.currentPage);

        } else {
            tableBody.innerHTML = `<tr><td colspan="7" class="px-6 py-4 text-center text-red-500">${data.message}</td></tr>`;
        }
    } catch (error) {
        console.error('Error loading products:', error);
        Notiflix.Notify.failure('Failed to load products');
        tableBody.innerHTML = '<tr><td colspan="7" class="px-6 py-4 text-center text-red-500">Failed to load products</td></tr>';
    }
}

function renderPagination(totalPages, currentPage) {
    const paginationContainer = document.getElementById('pagination-container');
    if (!paginationContainer) return;

    paginationContainer.innerHTML = '';

    // if (totalPages <= 1) return; // Always show pagination for better UX

    let html = '<div class="flex items-center gap-2 bg-black/40 p-1 rounded-lg border border-white/5">';

    // Previous Button
    const prevDisabled = currentPage === 1;
    const prevClass = prevDisabled ? 'text-zinc-600 cursor-not-allowed' : 'text-zinc-400 hover:text-white hover:bg-white/10';
    html += `
        <button onclick="loadProducts(${currentPage - 1})" ${prevDisabled ? 'disabled' : ''} 
            class="h-8 w-8 flex items-center justify-center rounded-md transition-all ${prevClass}">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
        </button>
    `;

    // Page Numbers
    let startPage = Math.max(1, currentPage - 2);
    let endPage = Math.min(totalPages, currentPage + 2);

    if (startPage > 1) {
        html += `
            <button onclick="loadProducts(1)" class="h-8 w-8 flex items-center justify-center rounded-md text-xs font-medium text-zinc-400 hover:text-white hover:bg-white/10 transition-all">1</button>
         `;
        if (startPage > 2) html += `<span class="h-8 w-8 flex items-center justify-center text-zinc-600 text-xs">...</span>`;
    }

    for (let i = startPage; i <= endPage; i++) {
        const activeClass = i === currentPage
            ? 'bg-nike-green text-black font-bold shadow-lg shadow-nike-green/20'
            : 'text-zinc-400 hover:text-white hover:bg-white/10';

        html += `<button onclick="loadProducts(${i})" 
            class="h-8 w-8 flex items-center justify-center rounded-md text-xs transition-all ${activeClass}">${i}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) html += `<span class="h-8 w-8 flex items-center justify-center text-zinc-600 text-xs">...</span>`;
        html += `
            <button onclick="loadProducts(${totalPages})" class="h-8 w-8 flex items-center justify-center rounded-md text-xs font-medium text-zinc-400 hover:text-white hover:bg-white/10 transition-all">${totalPages}</button>
         `;
    }

    // Next Button
    const nextDisabled = currentPage === totalPages;
    const nextClass = nextDisabled ? 'text-zinc-600 cursor-not-allowed' : 'text-zinc-400 hover:text-white hover:bg-white/10';
    html += `
        <button onclick="loadProducts(${currentPage + 1})" ${nextDisabled ? 'disabled' : ''} 
            class="h-8 w-8 flex items-center justify-center rounded-md transition-all ${nextClass}">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
        </button>
    `;

    html += '</div>';
    paginationContainer.innerHTML = html;
}

async function openProductModal(productId) {
    const modal = document.getElementById('product-modal');
    const content = document.getElementById('product-modal-content');

    try {
        const response = await fetch(`../api/admin/product/${productId}`);
        const data = await response.json();

        if (data.status) {
            const product = data.product;
            document.getElementById('edit-product-id').value = product.id;
            document.getElementById('edit-product-name').value = product.name;
            document.getElementById('edit-product-description').value = product.description;
            document.getElementById('edit-product-brand').value = product.brandName;
            document.getElementById('edit-product-model').value = product.modelName;

            // Set Status (Case Insensitive Match)
            const statusSelect = document.getElementById('edit-product-status');
            for (let i = 0; i < statusSelect.options.length; i++) {
                if (statusSelect.options[i].value.toLowerCase() === product.status.toLowerCase()) {
                    statusSelect.selectedIndex = i;
                    break;
                }
            }

            // Render Images
            const imagesContainer = document.getElementById('edit-product-images-container');
            imagesContainer.innerHTML = '';

            if (product.images && product.images.length > 0) {
                product.images.forEach(img => {
                    const imgDiv = document.createElement('div');
                    imgDiv.className = 'aspect-square rounded-lg bg-zinc-800 border border-white/10 overflow-hidden relative group';
                    imgDiv.innerHTML = `
                        <img src="../${img.path}" class="w-full h-full object-cover">
                        <button onclick="removeProductImage(${img.id}, ${product.id})" class="absolute top-1 right-1 p-1 bg-black/50 rounded-full text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-500">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    `;
                    imagesContainer.appendChild(imgDiv);
                });
            } else {
                imagesContainer.innerHTML = '<p class="text-xs text-zinc-500 col-span-4">No images available.</p>';
            }

            modal.classList.remove('hidden');
            setTimeout(() => {
                content.classList.remove('translate-x-full');
            }, 10);
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (error) {
        console.error('Error loading product details:', error);
        Notiflix.Notify.failure('Failed to load product details');
    }
}

async function addProductImage(input) {
    const productId = document.getElementById('edit-product-id').value;
    const files = input.files;

    if (files.length === 0) return;

    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append("images[]", files[i]);
    }

    Notiflix.Loading.pulse('Uploading...');

    try {
        const response = await fetch(`../api/admin/product/${productId}/upload-images`, {
            method: 'POST',
            body: formData
        });
        const data = await response.json();

        Notiflix.Loading.remove();

        if (data.status) {
            Notiflix.Notify.success('Images uploaded!');
            // Refresh modal to show new images
            openProductModal(productId);
            // Also refresh list to update thumbnail if needed
            loadProducts();
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (error) {
        Notiflix.Loading.remove();
        console.error('Error uploading images:', error);
        Notiflix.Notify.failure('Failed to upload images');
    }

    input.value = ''; // Reset input
}

async function removeProductImage(imageId, productId) {
    Notiflix.Confirm.show(
        'Delete Image',
        'Are you sure you want to delete this image?',
        'Yes',
        'No',
        async () => {
            try {
                const response = await fetch(`../api/admin/product/image/${imageId}`, {
                    method: 'DELETE'
                });
                const data = await response.json();

                if (data.status) {
                    Notiflix.Notify.success('Image deleted!');
                    openProductModal(productId); // Refresh modal
                    loadProducts(); // Refresh list
                } else {
                    Notiflix.Notify.failure(data.message);
                }
            } catch (error) {
                console.error('Error deleting image:', error);
                Notiflix.Notify.failure('Failed to delete image');
            }
        }
    );
}

function closeProductModal() {
    const modal = document.getElementById('product-modal');
    const content = document.getElementById('product-modal-content');
    content.classList.add('translate-x-full');
    setTimeout(() => {
        modal.classList.add('hidden');
    }, 300);
}

async function loadBrands() {
    const brandSelect = document.getElementById('brand-select');
    try {
        const response = await fetch('../api/admin/brand/list');
        const data = await response.json();

        if (data.status) {
            // Keep the first "Select Brand" option and the last "Add New Brand" option
            // We'll rebuild the options in between

            // Clear existing options except first and last (logic needs to be robust)
            // Easier approach: Clear all, add default, add fetched, add "Add New"

            brandSelect.innerHTML = `
                <option value="" disabled selected>Select Brand</option>
                ${data.brands.map(brand => `<option value="${brand.name}">${brand.name}</option>`).join('')}
                <option value="new">+ Add New Brand</option>
            `;
        }
    } catch (error) {
        console.error('Error loading brands:', error);
        Notiflix.Notify.failure('Failed to load brands');
    }
}

function closeAddProductModal() {
    const modal = document.getElementById('add-product-modal');
    const content = document.getElementById('add-product-modal-content');
    content.classList.add('translate-x-full');
    setTimeout(() => {
        modal.classList.add('hidden');
    }, 300);
}

// Image Upload Logic
function handleImageUpload(input) {
    const container = document.getElementById('image-preview-container');
    const files = Array.from(input.files);

    files.forEach(file => {
        if (file.type.startsWith('image/')) {
            const reader = new FileReader();

            reader.onload = function (e) {
                const id = Date.now() + Math.random().toString(36).substr(2, 9);
                const base64 = e.target.result;

                // Store file object for upload
                productImages.push({ id, base64, file });

                const previewDiv = document.createElement('div');
                previewDiv.id = `img-${id}`;
                previewDiv.className = 'aspect-square rounded-lg bg-zinc-800 border border-white/10 overflow-hidden relative group animate-fade-in';

                previewDiv.innerHTML = `
                    <img src="${base64}" class="w-full h-full object-cover">
                    <button onclick="removeImage('${id}')" class="absolute top-1 right-1 p-1 bg-black/50 rounded-full text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-500">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                `;

                // Insert before the upload button
                container.insertBefore(previewDiv, container.lastElementChild);
            };

            reader.readAsDataURL(file);
        }
    });

    // Reset input so same files can be selected again if needed
    input.value = '';
}

function removeImage(id) {
    productImages = productImages.filter(img => img.id !== id);
    const el = document.getElementById(`img-${id}`);
    if (el) el.remove();
}

// Brand Selection Logic
function toggleNewBrandInput(select) {
    const input = document.getElementById('new-brand-input');
    if (select.value === 'new') {
        input.classList.remove('hidden');
        input.focus();
    } else {
        input.classList.add('hidden');
    }
}

// Save Product Logic
async function saveNewProduct() {
    const name = document.getElementById('product-name').value;
    const description = document.getElementById('product-description').value;
    const brandSelect = document.getElementById('brand-select').value;
    const newBrandInput = document.getElementById('new-brand-input').value;
    const model = document.getElementById('product-model').value;
    const genderInput = document.querySelector('input[name="gender"]:checked');
    const gender = genderInput ? genderInput.value : null;

    const productDataObj = {
        name: name,
        description: description,
        modelName: model,
        gender: gender
    };

    if (brandSelect === 'new') {
        productDataObj.newBrandName = newBrandInput;
    } else {
        productDataObj.brandName = brandSelect;
    }

    const formData = new FormData();
    formData.append("product", JSON.stringify(productDataObj));

    Notiflix.Loading.pulse('Creating Product...');

    try {
        const response = await fetch('../api/admin/product/add', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (data.status) {
            if (productImages.length > 0) {
                await uploadProductImages(data.productId);
            } else {
                Notiflix.Loading.remove();
                Notiflix.Notify.success(data.message);
                closeAddProductModal();
                resetForm();
                loadProducts(1);
            }
        } else {
            Notiflix.Loading.remove();
            Notiflix.Notify.failure(data.message);
        }

    } catch (error) {
        Notiflix.Loading.remove();
        console.error('Error:', error);
        Notiflix.Notify.failure('Something went wrong. Please try again.');
    }
}

async function uploadProductImages(productId) {
    const formData = new FormData();
    productImages.forEach(img => {
        formData.append("images[]", img.file);
    });

    try {
        const response = await fetch(`../api/admin/product/${productId}/upload-images`, {
            method: 'POST',
            body: formData
        });

        const data = await response.json();
        Notiflix.Loading.remove();

        if (data.status) {
            Notiflix.Notify.success('Product and images saved successfully!');
            closeAddProductModal();
            resetForm();
        } else {
            Notiflix.Notify.failure(data.message);
        }

    } catch (error) {
        Notiflix.Loading.remove();
        console.error('Error:', error);
        Notiflix.Notify.failure('Failed to upload images.');
    }
}

function resetForm() {
    document.getElementById('add-product-form').reset();
    document.getElementById('new-brand-input').classList.add('hidden');

    // Reset images
    productImages = [];
    const container = document.getElementById('image-preview-container');
    // Remove all children except the last one (upload button)
    while (container.children.length > 1) {
        container.removeChild(container.firstChild);
    }
}

async function saveProductChanges() {
    const productId = document.getElementById('edit-product-id').value;
    const name = document.getElementById('edit-product-name').value;
    const description = document.getElementById('edit-product-description').value;
    const modelName = document.getElementById('edit-product-model').value;
    const status = document.getElementById('edit-product-status').value;

    const productDTO = {
        id: productId,
        name: name,
        description: description,
        modelName: modelName,
        status: status
    };

    try {
        const response = await fetch('../api/admin/product/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(productDTO)
        });
        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success(data.message);
            closeProductModal();
            loadProducts(currentPage);
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (error) {
        console.error('Error updating product:', error);
        Notiflix.Notify.failure('Failed to update product');
    }
}

// ==========================================
// Stock Management Logic
// ==========================================

let currentStockProductId = null;
let currentStockImages = []; // Store product images for the picker

async function openStockManagementModal() {
    const productId = document.getElementById('edit-product-id').value;
    if (!productId) return;
    currentStockProductId = productId;

    const modal = document.getElementById('stock-modal');
    modal.classList.remove('hidden');

    // Load available images for picker later
    try {
        const response = await fetch(`../api/admin/product/${productId}`);
        const data = await response.json();
        if (data.status) {
            currentStockImages = data.product.images;
        }
    } catch (e) { console.error("Error fetching images for picker", e); }

    loadProductStocks(productId);
}

function closeStockManagementModal() {
    document.getElementById('stock-modal').classList.add('hidden');
}

async function loadProductStocks(productId) {
    const tbody = document.getElementById('stock-table-body');
    tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-zinc-500">Loading stocks...</td></tr>';

    try {
        const response = await fetch(`../api/admin/product/${productId}/stocks`);
        const data = await response.json();

        if (data.status) {
            tbody.innerHTML = '';
            data.stocks.forEach(stock => {
                const imgDisplay = stock.variantImage
                    ? `<img src="../${stock.variantImage}" class="h-10 w-10 rounded border border-white/10 object-cover cursor-pointer hover:border-nike-green" onclick="openImagePicker(${stock.id})">`
                    : `<button onclick="openImagePicker(${stock.id})" class="h-10 w-10 rounded border border-dashed border-zinc-600 flex items-center justify-center text-xs text-zinc-500 hover:text-white hover:border-nike-green">+</button>`;

                const row = `
                    <tr class="hover:bg-white/5 transition-colors">
                        <td class="px-6 py-4">
                            <div class="text-white font-medium">${stock.color}</div>
                            <div class="text-xs text-zinc-500">${stock.size}</div>
                        </td>
                        <td class="px-6 py-4">${stock.qty}</td>
                        <td class="px-6 py-4">
                            <input type="number" id="stock-price-${stock.id}" value="${stock.sellingPrice}" 
                                class="bg-black/40 border border-white/10 rounded px-2 py-1 text-sm text-white w-24 focus:border-nike-green focus:outline-none">
                        </td>
                        <td class="px-6 py-4">
                            <select id="stock-status-${stock.id}" 
                                class="bg-black/40 border border-white/10 rounded px-2 py-1 text-sm text-white focus:border-nike-green focus:outline-none">
                                <option value="Active" ${stock.status === 'Active' ? 'selected' : ''}>Active</option>
                                <option value="Inactive" ${stock.status === 'Inactive' ? 'selected' : ''}>Inactive</option>
                            </select>
                        </td>
                        <td class="px-6 py-4">
                            ${imgDisplay}
                        </td>
                        <td class="px-6 py-4 text-right">
                            <button onclick="updateStockItem(${stock.id})" 
                                class="px-3 py-1 bg-nike-green/10 text-nike-green border border-nike-green/20 rounded text-xs font-bold hover:bg-nike-green hover:text-black transition-colors">
                                SAVE
                            </button>
                        </td>
                    </tr>
                `;
                tbody.insertAdjacentHTML('beforeend', row);
            });
        } else {
            tbody.innerHTML = `<tr><td colspan="6" class="px-6 py-4 text-center text-red-500">${data.message}</td></tr>`;
        }
    } catch (error) {
        console.error("Error loading stocks:", error);
        tbody.innerHTML = '<tr><td colspan="6" class="px-6 py-4 text-center text-red-500">Failed to load stocks</td></tr>';
    }
}

async function updateStockItem(stockId) {
    const price = document.getElementById(`stock-price-${stockId}`).value;
    const status = document.getElementById(`stock-status-${stockId}`).value;

    const dto = {
        id: stockId,
        sellingPrice: parseFloat(price),
        status: status
    };

    Notiflix.Loading.pulse('Updating Stock...');

    try {
        const response = await fetch('../api/admin/stock/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dto)
        });
        const data = await response.json();
        Notiflix.Loading.remove();

        if (data.status) {
            Notiflix.Notify.success('Stock updated!');
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (error) {
        Notiflix.Loading.remove();
        console.error(error);
        Notiflix.Notify.failure('Update failed');
    }
}

// Image Picker
let pickingForStockId = null;

function openImagePicker(stockId) {
    pickingForStockId = stockId;
    const modal = document.getElementById('image-picker-modal');
    const container = document.getElementById('image-picker-grid');
    container.innerHTML = '';

    if (currentStockImages.length === 0) {
        container.innerHTML = '<p class="col-span-4 text-center text-zinc-500">No images available for this product.</p>';
    } else {
        currentStockImages.forEach(img => {
            const div = document.createElement('div');
            div.className = 'aspect-square rounded bg-black/50 border border-white/10 overflow-hidden cursor-pointer hover:border-nike-green transition-all';
            div.innerHTML = `<img src="../${img.path}" class="w-full h-full object-cover">`;
            div.onclick = () => selectStockImage(img.id);
            container.appendChild(div);
        });
    }

    modal.classList.remove('hidden');
}

function closeImagePicker() {
    document.getElementById('image-picker-modal').classList.add('hidden');
    pickingForStockId = null;
}

async function selectStockImage(imageId) {
    if (!pickingForStockId) return;

    Notiflix.Loading.pulse('Assigning Image...');
    try {
        const params = new URLSearchParams();
        params.append('stockId', pickingForStockId);
        params.append('imageId', imageId);

        const response = await fetch('../api/admin/stock/assign-image', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params
        });
        const data = await response.json();
        Notiflix.Loading.remove();

        if (data.status) {
            Notiflix.Notify.success('Image linked!');
            closeImagePicker();
            loadProductStocks(currentStockProductId); // Refresh table
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (e) {
        Notiflix.Loading.remove();
        console.error(e);
        Notiflix.Notify.failure('Failed to assign image');
    }
}
