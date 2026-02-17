// Global variables
let grnItems = []; // Buffer for NEW GRN creation
let currentViewingGrnId = null; // ID of GRN currently open in Detail Modal

document.addEventListener('DOMContentLoaded', () => {
    loadSuppliers();
    loadProducts();
    loadColors();
    loadSizes();
    loadGrnList(); // New List Loader

    // Auto-update total on creation form
    updateGrnItemsTable();
});

// ==========================================
// Shared Loaders (Dropdowns)
// ==========================================
async function loadSuppliers() {
    const select = document.getElementById('grn-supplier-select');
    try {
        const response = await fetch('../api/admin/supplier/list');
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                let options = '<option value="" disabled selected>Choose a supplier...</option>';
                data.suppliers.forEach(s => options += `<option value="${s.id}">${s.companyName}</option>`);
                if (select) select.innerHTML = options;
            }
        }
    } catch (e) { console.error(e); }
}

async function loadProducts() {
    const selects = [document.getElementById('grn-product-select'), document.getElementById('add-item-product'), document.getElementById('edit-product-select')];
    try {
        const response = await fetch('../api/admin/product/list');
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                let options = '<option value="" disabled selected>Choose Product...</option>';
                data.products.forEach(p => options += `<option value="${p.id}">${p.name}</option>`);
                selects.forEach(s => { if (s) s.innerHTML = options; });
            }
        }
    } catch (e) { console.error(e); }
}

async function loadColors() {
    const selects = [document.getElementById('grn-color-select'), document.getElementById('add-item-color'), document.getElementById('edit-color-select')];
    try {
        const response = await fetch('../api/admin/color/list');
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                let options = '<option value="" disabled selected>Choose Color...</option>';
                data.colors.forEach(c => options += `<option value="${c.id}">${c.name}</option>`);
                selects.forEach(s => { if (s) s.innerHTML = options; });
            }
        }
    } catch (e) { console.error(e); }
}

async function loadSizes() {
    const selects = [document.getElementById('grn-size-select'), document.getElementById('add-item-size'), document.getElementById('edit-size-select')];
    try {
        const response = await fetch('../api/admin/size/list');
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                let options = '<option value="" disabled selected>Choose Size...</option>';
                data.sizes.forEach(s => options += `<option value="${s.id}">${s.name}</option>`);
                selects.forEach(s => { if (s) s.innerHTML = options; });
            }
        }
    } catch (e) { console.error(e); }
}

// ==========================================
// 1. GRN Invoice List (New View)
// ==========================================
let currentPage = 1;

function loadGrnList(page = 1) {
    currentPage = page;
    const container = document.getElementById('grn-list-container');
    const pagination = document.getElementById('pagination-container');
    if (!container) return; // Guard

    container.innerHTML = '<div class="text-white col-span-3 text-center animate-pulse">Loading Invoices...</div>';

    fetch(`../api/admin/grn/list?page=${page}`)
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                container.innerHTML = '';
                if (data.grns.length === 0) {
                    container.innerHTML = `
                        <div class="col-span-3 glass-panel p-12 rounded-3xl text-center border border-white/10">
                            <div class="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4">
                                <i class="fas fa-file-invoice text-2xl text-zinc-600"></i>
                            </div>
                            <h3 class="text-xl font-bold mb-2">No GRNs Found</h3>
                            <p class="text-gray-400">Start by creating a new GRN above.</p>
                        </div>
                    `;
                    return;
                }

                data.grns.forEach(grn => {
                    const card = `
                        <div class="glass-panel p-6 rounded-3xl space-y-4 border border-white/5 hover:border-green-500/30 transition-all group z-10 relative">
                            
                            <div class="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                                <div class="flex items-center gap-4">
                                    <div class="w-12 h-12 rounded-xl bg-green-500/20 flex items-center justify-center text-green-500">
                                        <i class="fas fa-file-invoice-dollar"></i>
                                    </div>
                                    <div>
                                        <h3 class="font-bold text-lg">GRN #${grn.id}</h3>
                                        <p class="text-sm text-gray-400">${grn.date}</p>
                                    </div>
                                </div>

                                <div class="flex items-center gap-4 self-end md:self-auto">
                                    <div class="px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider border text-green-500 bg-green-500/10 border-green-500/20">
                                        Received
                                    </div>
                                    <button onclick="openGrnDetails(${grn.id})" 
                                        class="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 transition-all text-sm font-bold flex items-center gap-2 cursor-pointer z-20 relative">
                                        Details <i class="fas fa-arrow-right"></i>
                                    </button>
                                </div>
                            </div>

                            <!-- Content Body (Supplier Info instead of Items) -->
                            <div class="w-full space-y-4 pt-4 border-t border-white/5">
                                <div class="flex items-center gap-4 bg-white/5 p-3 rounded-xl">
                                    <div class="w-12 h-12 rounded-lg bg-white/5 overflow-hidden flex-shrink-0 flex items-center justify-center text-zinc-500">
                                         <i class="fas fa-store"></i>
                                    </div>
                                    <div class="flex-1">
                                        <h4 class="font-bold text-sm text-zinc-300">Supplier</h4>
                                        <p class="text-sm font-semibold text-white">${grn.supplier}</p>
                                    </div>
                                    <div class="text-right">
                                         <h4 class="font-bold text-sm text-zinc-300">Inv. Ref</h4>
                                         <p class="text-sm font-mono text-zinc-400">${grn.supplierInvoice}</p>
                                    </div>
                                </div>
                            </div>

                            <div class="flex justify-between items-center pt-4 border-t border-white/10">
                                <div class="flex items-center gap-2">
                                    <span class="text-sm text-gray-400">Items:</span>
                                    <span class="text-sm font-bold text-white">${grn.itemCount}</span>
                                </div>
                                <div class="flex items-center gap-2">
                                     <span class="text-sm text-gray-400">Total:</span>
                                     <span class="text-xl font-bold text-green-400">Rs.${grn.total.toFixed(2)}</span>
                                </div>
                            </div>

                        </div>
                    `;
                    container.insertAdjacentHTML('beforeend', card);
                });

                renderPagination(data.totalPages, data.currentPage);
            } else {
                container.innerHTML = '<div class="text-red-500 col-span-3 text-center">Failed to load invoices.</div>';
            }
        })
        .catch(err => {
            console.error(err);
            container.innerHTML = '<div class="text-red-500 col-span-3 text-center">Error loading invoices.</div>';
        });
}

function renderPagination(totalPages, current) {
    const container = document.getElementById('pagination-container');
    if (!container) return;

    let html = '';
    html += `<button onclick="loadGrnList(${current - 1})" ${current === 1 ? 'disabled class="opacity-50"' : ''} class="text-zinc-400 hover:text-white px-2">Prev</button>`;
    html += `<span class="text-zinc-500 px-2">Page ${current} of ${totalPages}</span>`;
    html += `<button onclick="loadGrnList(${current + 1})" ${current === totalPages ? 'disabled class="opacity-50"' : ''} class="text-zinc-400 hover:text-white px-2">Next</button>`;
    container.innerHTML = html;
}

// ==========================================
// 2. GRN Detail Modal
// ==========================================
function openGrnDetails(id) {
    currentViewingGrnId = id;
    const modal = document.getElementById('grn-detail-modal');
    const tbody = document.getElementById('detail-items-body');
    const totalEl = document.getElementById('detail-grand-total');

    // Reset UI
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4">Loading...</td></tr>';
    document.getElementById('detail-grn-id').textContent = id;
    modal.classList.remove('hidden');

    fetch(`../api/admin/grn/details/${id}`)
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                const grn = data.data;
                document.getElementById('detail-supplier').textContent = grn.supplier;
                document.getElementById('detail-date').textContent = grn.date;
                document.getElementById('detail-invoice-no').textContent = grn.supplierInvoice;
                totalEl.textContent = '$' + grn.grandTotal.toFixed(2);

                if (grn.items.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-zinc-500">No items in this invoice.</td></tr>';
                } else {
                    tbody.innerHTML = '';
                    grn.items.forEach(item => {
                        const tr = `
                            <tr class="hover:bg-white/5 transition-colors">
                                <td class="px-6 py-3">
                                    <div class="w-12 h-12 rounded bg-white/5 overflow-hidden">
                                        <img src="../${item.imagePath}" alt="Product" class="w-full h-full object-cover">
                                    </div>
                                </td>
                                <td class="px-6 py-3 text-white">${item.productName}</td>
                                <td class="px-6 py-3 text-zinc-400">${item.colorName}</td>
                                <td class="px-6 py-3 text-zinc-400">${item.sizeName}</td>
                                <td class="px-6 py-3 font-bold text-white">${item.qty}</td>
                                <td class="px-6 py-3">Rs.${item.buyingPrice.toFixed(2)}</td>
                                <td class="px-6 py-3 text-zinc-400">Rs.${item.sellingPrice.toFixed(2)}</td>
                                <td class="px-6 py-3 font-mono">Rs.${item.total.toFixed(2)}</td>
                                <td class="px-6 py-3 text-center">
                                     <button onclick="editGrnItem(${item.id})" class="text-xs bg-nike-green/10 text-nike-green px-2 py-1 rounded border border-nike-green/20 hover:bg-nike-green hover:text-black transition-colors mr-2">Edit</button>
                                     <button onclick="deleteGrnItem(${item.id})" class="text-xs bg-red-500/10 text-red-500 px-2 py-1 rounded border border-red-500/20 hover:bg-red-500 hover:text-white transition-colors">Delete</button>
                                    ${item.hasSales ? '<span class="block text-[10px] text-orange-500 mt-1">Has Sales</span>' : ''}
                                </td>
                            </tr>
                        `;
                        tbody.insertAdjacentHTML('beforeend', tr);
                    });
                }
            } else {
                Notiflix.Notify.failure(data.message);
                closeGrnDetailModal();
            }
        })
        .catch(err => {
            console.error(err);
            Notiflix.Notify.failure("Error loading details");
            closeGrnDetailModal();
        });
}

function closeGrnDetailModal() {
    document.getElementById('grn-detail-modal').classList.add('hidden');
    currentViewingGrnId = null;
    loadGrnList(currentPage); // Refresh list on close to update totals/counts
}

// Add Item to EXISTING GRN
function addItemToGrn() {
    if (!currentViewingGrnId) return;

    const productId = document.getElementById('add-item-product').value;
    const colorId = document.getElementById('add-item-color').value;
    const sizeId = document.getElementById('add-item-size').value;
    const qty = parseInt(document.getElementById('add-item-qty').value);
    const buy = parseFloat(document.getElementById('add-item-buy').value);
    const sell = parseFloat(document.getElementById('add-item-sell').value);

    // Basic Validations
    if (!productId || !colorId || !sizeId) { Notiflix.Notify.warning("Select Product, Color, and Size"); return; }
    if (!qty || qty <= 0) { Notiflix.Notify.warning("Invalid Quantity"); return; }
    if (isNaN(buy) || buy < 0) { Notiflix.Notify.warning("Invalid Buying Price"); return; }
    if (isNaN(sell) || sell < 0) { Notiflix.Notify.warning("Invalid Selling Price"); return; }

    const payload = {
        grnItemId: currentViewingGrnId, // HACK: Using ID field to pass GRN ID as per backend implementation assumption
        productId: productId,
        colorId: colorId,
        sizeId: sizeId,
        quantity: qty,
        buyingPrice: buy,
        sellingPrice: sell
    };

    fetch('../api/admin/grn/item/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                Notiflix.Notify.success("Item Added!");
                // Clear Inputs
                document.getElementById('add-item-qty').value = '';
                document.getElementById('add-item-buy').value = '';
                document.getElementById('add-item-sell').value = '';
                // Refresh
                openGrnDetails(currentViewingGrnId);
            } else {
                Notiflix.Notify.failure(data.message);
            }
        });
}

function deleteCurrentGrn() {
    if (!currentViewingGrnId) return;
    Notiflix.Confirm.show(
        'Delete GRN',
        'Are you absolutely sure? This will delete the Invoice and ALL unsold items. If any item is sold, this will fail.',
        'Yes, Delete', 'Cancel',
        () => {
            fetch(`../api/admin/grn/delete/${currentViewingGrnId}`, { method: 'DELETE' })
                .then(res => res.json())
                .then(data => {
                    if (data.status) {
                        Notiflix.Notify.success("GRN Deleted");
                        closeGrnDetailModal();
                    } else {
                        Notiflix.Notify.failure(data.message);
                    }
                });
        }
    );
}

// ==========================================
// 3. Edit / Delete Single Item (in Modal Context)
// ==========================================
function editGrnItem(id) {
    // 1. Fetch Details & Show Sub-Modal (reusing existing modal/logic)
    fetch(`../api/admin/grn/item-details/${id}`)
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                const item = data.data;
                document.getElementById('edit-item-id').value = item.id;
                document.getElementById('edit-qty').value = item.qty;
                document.getElementById('edit-buying-price').value = item.buyingPrice;
                document.getElementById('edit-selling-price').value = item.sellingPrice || '';

                // Populate Dropdowns in Edit Modal using innerHTML of existing selects
                const pOpt = document.getElementById('add-item-product').innerHTML;
                document.getElementById('edit-product-select').innerHTML = pOpt;
                document.getElementById('edit-product-select').value = item.productId;

                const cOpt = document.getElementById('add-item-color').innerHTML;
                document.getElementById('edit-color-select').innerHTML = cOpt;
                document.getElementById('edit-color-select').value = item.colorId;

                const sOpt = document.getElementById('add-item-size').innerHTML;
                document.getElementById('edit-size-select').innerHTML = sOpt;
                document.getElementById('edit-size-select').value = item.sizeId;

                document.getElementById('edit-modal').classList.remove('hidden');
            } else {
                Notiflix.Notify.failure(data.message);
            }
        });
}

function saveEditedItem() {
    // Collect Data
    const id = document.getElementById('edit-item-id').value;
    const qty = parseInt(document.getElementById('edit-qty').value);
    const buy = parseFloat(document.getElementById('edit-buying-price').value);
    const sell = parseFloat(document.getElementById('edit-selling-price').value);
    const pid = document.getElementById('edit-product-select').value;
    const cid = document.getElementById('edit-color-select').value;
    const sid = document.getElementById('edit-size-select').value;

    // Validation
    if (qty <= 0) { Notiflix.Notify.warning("Invalid Qty"); return; }
    if (buy < 0) { Notiflix.Notify.warning("Invalid Buy Price"); return; }
    if (sell < 0) { Notiflix.Notify.warning("Invalid Sell Price"); return; }

    const payload = {
        grnItemId: id, quantity: qty, buyingPrice: buy, sellingPrice: sell,
        productId: pid, colorId: cid, sizeId: sid
    };

    fetch('../api/admin/grn/item/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                Notiflix.Notify.success("Item Updated");
                closeEditModal();
                // Refresh Detail View
                if (currentViewingGrnId) openGrnDetails(currentViewingGrnId);
            } else {
                Notiflix.Notify.failure(data.message);
            }
        });
}

function deleteGrnItem(id) {
    Notiflix.Confirm.show('Delete Item', 'Remove this line item?', 'Yes', 'No', () => {
        fetch(`../api/admin/grn/item/${id}`, { method: 'DELETE' })
            .then(res => res.json())
            .then(data => {
                if (data.status) {
                    Notiflix.Notify.success("Item Deleted");
                    if (currentViewingGrnId) openGrnDetails(currentViewingGrnId);
                } else {
                    Notiflix.Notify.failure(data.message);
                }
            });
    });
}
function closeEditModal() { document.getElementById('edit-modal').classList.add('hidden'); }


// ==========================================
// 4. Create New GRN (Creation Form Logic)
// ==========================================
// (Keeping existing logic mostly as is, just function names exposed)

function addGrnItem() { // Adds to BUFFER for new GRN
    const pSel = document.getElementById('grn-product-select');
    const cSel = document.getElementById('grn-color-select');
    const sSel = document.getElementById('grn-size-select');
    const qIn = document.getElementById('grn-qty-input');
    const bIn = document.getElementById('grn-buying-price-input');
    const sIn = document.getElementById('grn-selling-price-input');

    if (!pSel.value || !cSel.value || !sSel.value || !qIn.value || !bIn.value) {
        Notiflix.Notify.warning("Fill all fields"); return;
    }

    const item = {
        productId: pSel.value, productTitle: pSel.options[pSel.selectedIndex].text,
        colorId: cSel.value, colorName: cSel.options[cSel.selectedIndex].text,
        sizeId: sSel.value, sizeName: sSel.options[sSel.selectedIndex].text,
        quantity: parseInt(qIn.value),
        buyingPrice: parseFloat(bIn.value),
        sellingPrice: parseFloat(sIn.value || 0),
        total: parseInt(qIn.value) * parseFloat(bIn.value)
    };

    grnItems.push(item);
    updateGrnItemsTable();

    // Reset inputs
    qIn.value = ''; bIn.value = ''; sIn.value = '';
}

function removeGrnItem(idx) {
    grnItems.splice(idx, 1);
    updateGrnItemsTable();
}

function updateGrnItemsTable() {
    const tbody = document.getElementById('grn-items-table-body');
    if (!tbody) return;
    tbody.innerHTML = '';
    let total = 0;
    grnItems.forEach((it, idx) => {
        total += it.total;
        tbody.insertAdjacentHTML('beforeend', `
            <tr class="hover:bg-white/5 border-b border-white/5">
                <td class="px-4 py-2 text-white">${it.productTitle}</td>
                <td class="px-4 py-2">${it.colorName}</td>
                <td class="px-4 py-2">${it.sizeName}</td>
                <td class="px-4 py-2">${it.quantity}</td>
                <td class="px-4 py-2">Rs.${it.buyingPrice}</td>
                <td class="px-4 py-2">Rs.${it.total}</td>
                <td class="px-4 py-2"><button onclick="removeGrnItem(${idx})" class="text-red-500">x</button></td>
            </tr>
        `);
    });
    const totalEl = document.getElementById('grn-total-amount');
    if (totalEl) totalEl.innerText = '$' + total.toFixed(2);
}

async function saveGrn() {
    const supplierId = document.getElementById('grn-supplier-select').value;
    const invoice = document.getElementById('grn-invoice-number').value;

    if (!supplierId || grnItems.length === 0) { Notiflix.Notify.warning("Select Supplier and add items"); return; }

    const payload = {
        supplierId: supplierId,
        supplierInvoiceNumber: invoice,
        totalAmount: 0, // calc on server? or 
        grnItems: grnItems
    };

    fetch('../api/admin/grn/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            if (data.status) {
                Notiflix.Notify.success("GRN Created!");
                grnItems = [];
                updateGrnItemsTable();
                document.getElementById('grn-form').reset();
                loadGrnList(1); // Refresh Invoice List
            } else {
                Notiflix.Notify.failure(data.message);
            }
        });
}

// Expose
window.loadGrnList = loadGrnList;
window.openGrnDetails = openGrnDetails;
window.closeGrnDetailModal = closeGrnDetailModal;
window.addItemToGrn = addItemToGrn;
window.deleteCurrentGrn = deleteCurrentGrn;
window.editGrnItem = editGrnItem;
window.saveEditedItem = saveEditedItem;
window.deleteGrnItem = deleteGrnItem;
window.closeEditModal = closeEditModal;
window.addGrnItem = addGrnItem;
window.removeGrnItem = removeGrnItem;
window.saveGrn = saveGrn;
