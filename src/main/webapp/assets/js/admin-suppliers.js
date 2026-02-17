// Global variable to store supplier data if needed (optional)
let suppliers = [];

document.addEventListener('DOMContentLoaded', () => {
    loadSuppliers();
});

// Expose functions to global scope
window.openAddSupplierModal = openAddSupplierModal;
window.closeAddSupplierModal = closeAddSupplierModal;
window.saveNewSupplier = saveNewSupplier;
window.openEditSupplierModal = openEditSupplierModal;
window.closeEditSupplierModal = closeEditSupplierModal;
window.saveSupplierChanges = saveSupplierChanges;

// Load Suppliers
async function loadSuppliers() {
    const tableBody = document.getElementById('supplier-table-body');
    if (!tableBody) return;

    // Show loading state (optional UI enhancement)
    // tableBody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-zinc-500">Loading...</td></tr>';

    try {
        const response = await fetch('../api/admin/supplier/list');
        const data = await response.json();

        if (data.status) {
            tableBody.innerHTML = ''; // Clear table
            suppliers = data.suppliers; // Store for local access if needed

            if (suppliers.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="6" class="text-center py-8 text-zinc-500">No suppliers found.</td></tr>';
                return;
            }

            suppliers.forEach(supplier => {
                const statusColor = supplier.status.toLowerCase() === 'active' ? 'emerald' : 'red';
                const row = `
                    <tr class="hover:bg-white/5 transition-colors group">
                        <td class="px-6 py-4 font-medium text-white">${supplier.companyName}</td>
                        <td class="px-6 py-4">${supplier.companyEmail}</td>
                        <td class="px-6 py-4">${supplier.companyMobile}</td>
                        <td class="px-6 py-4">
                            <span class="px-2 py-1 rounded text-[10px] font-bold bg-${statusColor}-500/10 text-${statusColor}-500 border border-${statusColor}-500/20">
                                ${supplier.status.toUpperCase()}
                            </span>
                        </td>
                        <td class="px-6 py-4 text-right">
                             <button onclick="openEditSupplierModal(${supplier.id})" class="text-zinc-400 hover:text-white p-2">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                </svg>
                            </button>
                        </td>
                    </tr>
                `;
                tableBody.insertAdjacentHTML('beforeend', row);
            });
        } else {
            tableBody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-red-500">${data.message}</td></tr>`;
        }
    } catch (error) {
        console.error('Error loading suppliers:', error);
        Notiflix.Notify.failure('Failed to load suppliers');
    }
}

// Add Supplier Modal
function openAddSupplierModal() {
    const modal = document.getElementById('add-supplier-modal');
    const content = document.getElementById('add-supplier-modal-content');
    modal.classList.remove('hidden');
    setTimeout(() => {
        content.classList.remove('translate-x-full');
    }, 10);
    document.getElementById('add-supplier-form').reset();
}

function closeAddSupplierModal() {
    const modal = document.getElementById('add-supplier-modal');
    const content = document.getElementById('add-supplier-modal-content');
    content.classList.add('translate-x-full');
    setTimeout(() => {
        modal.classList.add('hidden');
    }, 300);
}

async function saveNewSupplier() {
    const name = document.getElementById('add-supplier-name').value;
    const mobile = document.getElementById('add-supplier-mobile').value;
    const email = document.getElementById('add-supplier-email').value;

    const supplierObj = {
        companyName: name,
        companyMobile: mobile,
        companyEmail: email
    };

    try {
        const response = await fetch('../api/admin/supplier/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(supplierObj)
        });
        if (response.ok) {
            const data = await response.json();

            if (data.status) {
                Notiflix.Notify.success(data.message);
                closeAddSupplierModal();
                loadSuppliers();
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure('Failed to add supplier');
        }
    } catch (e) {
        console.log('Error adding supplier:', e);
        Notiflix.Notify.failure('Failed to add supplier');
    }
}

// Edit Supplier Modal
async function openEditSupplierModal(id) {
    const modal = document.getElementById('edit-supplier-modal');
    const content = document.getElementById('edit-supplier-modal-content');

    try {
        const response = await fetch(`../api/admin/supplier/${id}`);
        const data = await response.json();

        if (data.status) {
            const supplier = data.supplier;
            document.getElementById('edit-supplier-id').value = supplier.id;
            document.getElementById('edit-supplier-name').value = supplier.companyName;
            document.getElementById('edit-supplier-mobile').value = supplier.companyMobile;
            document.getElementById('edit-supplier-email').value = supplier.companyEmail;

            // Set Status
            const statusSelect = document.getElementById('edit-supplier-status');
            for (let i = 0; i < statusSelect.options.length; i++) {
                if (statusSelect.options[i].value.toLowerCase() === supplier.status.toLowerCase()) {
                    statusSelect.selectedIndex = i;
                    break;
                }
            }

            modal.classList.remove('hidden');
            setTimeout(() => {
                content.classList.remove('translate-x-full');
            }, 10);
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (error) {
        console.error('Error loading supplier details:', error);
        Notiflix.Notify.failure('Failed to load supplier details');
    }
}

function closeEditSupplierModal() {
    const modal = document.getElementById('edit-supplier-modal');
    const content = document.getElementById('edit-supplier-modal-content');
    content.classList.add('translate-x-full');
    setTimeout(() => {
        modal.classList.add('hidden');
    }, 300);
}

async function saveSupplierChanges() {
    const id = document.getElementById('edit-supplier-id').value;
    const name = document.getElementById('edit-supplier-name').value;
    const mobile = document.getElementById('edit-supplier-mobile').value;
    const email = document.getElementById('edit-supplier-email').value;
    const status = document.getElementById('edit-supplier-status').value;

    const supplierDTO = {
        id: id,
        companyName: name,
        companyMobile: mobile,
        companyEmail: email,
        status: status
    };

    try {
        const response = await fetch('../api/admin/supplier/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(supplierDTO)
        });
        const data = await response.json();

        if (data.status) {
            Notiflix.Notify.success(data.message);
            closeEditSupplierModal();
            loadSuppliers();
        } else {
            Notiflix.Notify.failure(data.message);
        }
    } catch (error) {
        console.error('Error updating supplier:', error);
        Notiflix.Notify.failure('Failed to update supplier');
    }
}
