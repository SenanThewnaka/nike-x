const API_BASE_URL = '../api';
let currentPage = 1;

document.addEventListener('DOMContentLoaded', () => {
    loadOrders(currentPage);
});

async function loadOrders(page = 1) {
    currentPage = page;
    const tbody = document.getElementById('orders-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-gray-500">Loading orders...</td></tr>';

    try {
        const response = await fetch(`${API_BASE_URL}/admin/orders?page=${page}`);
        const data = await response.json();

        if (data.status) {
            const orders = data.orders;
            const totalPages = data.totalPages;

            console.log("Orders Data:", orders); // Debug Log

            if (orders.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-gray-500">No orders found.</td></tr>';
                renderPagination(0, page);
                return;
            }

            tbody.innerHTML = orders.map(order => {
                const status = order.status || 'Pending'; // Default
                const availableStatuses = ['Pending', 'Packing', 'Delivered', 'Completed', 'Canceled'];

                // Determine text color for select based on status
                let statusColorClass = 'text-white';
                if (status === 'Pending') statusColorClass = 'text-yellow-500 border-yellow-500/50';
                else if (status === 'Completed' || status === 'Delivered') statusColorClass = 'text-emerald-500 border-emerald-500/50';
                else if (status === 'Canceled') statusColorClass = 'text-red-500 border-red-500/50';
                else if (status === 'Packing') statusColorClass = 'text-blue-500 border-blue-500/50';

                const options = availableStatuses.map(s =>
                    `<option value="${s.toUpperCase()}" ${status.toUpperCase() === s.toUpperCase() ? 'selected' : ''}>${s.toUpperCase()}</option>`
                ).join('');

                const productName = order.firstProductName || 'Unknown Product';
                const extraItems = (order.itemCount || 0) > 1 ? ` <span class="text-zinc-500 font-normal ml-1 text-[10px]">(+${order.itemCount - 1} others)</span>` : '';

                let imagePath = order.firstProductImage || 'assets/images/placeholder.png';
                if (!imagePath.startsWith('../') && !imagePath.startsWith('http')) {
                    imagePath = '../' + imagePath;
                }

                const customerName = order.customerName || 'Guest';

                return `
                <tr class="hover:bg-white/5 transition-colors">
                    <td class="px-6 py-4 font-medium text-white">#${order.id}</td>
                    <td class="px-6 py-4">
                        <div class="flex items-center gap-3">
                             <div class="w-10 h-10 rounded-lg bg-zinc-800 flex items-center justify-center overflow-hidden border border-white/10 shrink-0">
                                ${order.firstProductImage ?
                        `<img src="${imagePath}" class="w-full h-full object-cover">` :
                        `<i class="fas fa-box text-zinc-500"></i>`
                    }
                            </div>
                            <div>
                                <p class="text-white text-sm font-medium">${customerName}</p>
                                <p class="text-xs text-zinc-400 mt-0.5">${productName}${extraItems}</p>
                            </div>
                        </div>
                    </td>
                    <td class="px-6 py-4 text-zinc-300 text-sm">${order.date}</td>
                    <td class="px-6 py-4 text-white font-medium">${formatCurrency(order.totalAmount || 0)}</td>
                    <td class="px-6 py-4">
                        <select onchange="updateOrderStatus(${order.id}, this.value, this)"
                            class="bg-black/20 border border-white/10 ${statusColorClass} text-xs rounded px-2 py-1 focus:outline-none focus:border-nike-green cursor-pointer">
                            ${options}
                        </select>
                    </td>
                    <td class="px-6 py-4 text-right">
                        <button onclick="viewOrderDetails(${order.id})" class="text-zinc-400 hover:text-white p-2" title="View Details">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                            </svg>
                        </button>
                    </td>
                </tr>
                `;
            }).join('');

            renderPagination(totalPages, page);
        } else {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-red-500">${data.message}</td></tr>`;
        }
    } catch (e) {
        console.error("Error loading orders", e);
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-red-500">Error loading orders.</td></tr>';
    }
}

async function viewOrderDetails(orderId) {
    Notiflix.Loading.pulse('Fetching details...');
    try {
        const response = await fetch(`${API_BASE_URL}/admin/order/${orderId}`);
        const data = await response.json();
        Notiflix.Loading.remove();

        if (data.status) {
            openOrderModal(data.order);
        } else {
            Notiflix.Notify.failure(data.message || 'Failed to load details');
        }
    } catch (e) {
        Notiflix.Loading.remove();
        console.error("Error fetching details", e);
        Notiflix.Notify.failure('Error fetching order details');
    }
}

function openOrderModal(order) {
    const modal = document.getElementById('order-modal');
    const backdrop = document.getElementById('order-modal-backdrop');
    const panel = document.getElementById('order-modal-panel');

    // Populate Fields
    document.getElementById('modal-order-id').innerText = `Order #${order.id}`;
    document.getElementById('modal-order-date').innerText = order.date || 'N/A';
    document.getElementById('modal-customer-name').innerText = order.customerName || 'Guest';
    document.getElementById('modal-customer-email').innerText = order.customerEmail || 'No email';

    // Address
    let addressHtml = `${order.addressLine1 || ''}<br>`;
    if (order.addressLine2) addressHtml += `${order.addressLine2}<br>`;
    addressHtml += `${order.city || ''}, ${order.postalCode || ''}`;
    document.getElementById('modal-address').innerHTML = addressHtml;

    document.getElementById('modal-payment-method').innerText = order.paymentMethod || 'N/A';
    document.getElementById('modal-total-amount').innerText = formatCurrency(order.totalAmount || 0);

    // Status Badge Style
    const statusBadge = document.getElementById('modal-status-badge');
    statusBadge.innerText = order.status || 'Unknown';

    let statusClasses = 'bg-zinc-500/10 text-zinc-500 border-zinc-500/20'; // Default
    const s = (order.status || '').toUpperCase();
    if (s === 'PENDING') statusClasses = 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20';
    else if (s === 'COMPLETED' || s === 'DELIVERED') statusClasses = 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20';
    else if (s === 'CANCELED') statusClasses = 'bg-red-500/10 text-red-500 border-red-500/20';
    else if (s === 'PACKING') statusClasses = 'bg-blue-500/10 text-blue-500 border-blue-500/20';

    statusBadge.className = `inline-flex w-fit items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${statusClasses}`;

    // Items
    const tbody = document.getElementById('modal-items-body');
    const items = order.items || [];

    if (items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="px-6 py-4 text-center text-zinc-500 text-sm">No items found</td></tr>';
    } else {
        tbody.innerHTML = items.map(item => {
            // Fix image path relative to admin/
            let displayImage = item.imagePath || 'assets/images/placeholder.png';
            if (!displayImage.startsWith('../') && !displayImage.startsWith('http')) {
                displayImage = '../' + displayImage;
            }

            return `
            <tr>
                <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                        <div class="flex-shrink-0 h-10 w-10 bg-zinc-800 rounded-md overflow-hidden border border-white/5">
                            <img class="h-10 w-10 object-cover" src="${displayImage}" alt="">
                        </div>
                        <div class="ml-4">
                            <div class="text-sm font-medium text-white">${item.productName || 'Unknown Product'}</div>
                            <div class="text-xs text-zinc-500">${item.variance || ''}</div>
                        </div>
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm text-zinc-400">
                    ${formatCurrency(item.price || 0)}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm text-zinc-300">
                    ${item.qty || 0}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium text-white">
                    ${formatCurrency(item.total || 0)}
                </td>
            </tr>
        `;
        }).join('');
    }

    // Show Modal (Animation logic)
    modal.classList.remove('hidden');
    // Small delay to allow display:block to apply before opacity transition
    setTimeout(() => {
        backdrop.classList.remove('opacity-0');
        panel.classList.remove('opacity-0', 'scale-95');
        panel.classList.add('opacity-100', 'scale-100');
    }, 10);
}

function closeOrderModal() {
    const modal = document.getElementById('order-modal');
    const backdrop = document.getElementById('order-modal-backdrop');
    const panel = document.getElementById('order-modal-panel');

    backdrop.classList.add('opacity-0');
    panel.classList.remove('opacity-100', 'scale-100');
    panel.classList.add('opacity-0', 'scale-95');

    setTimeout(() => {
        modal.classList.add('hidden');
    }, 300); // Match transition duration
}


async function updateOrderStatus(orderId, newStatus, selectElement) {
    Notiflix.Loading.pulse('Updating Status...');

    try {
        const formData = new URLSearchParams();
        formData.append('orderId', orderId);
        formData.append('status', newStatus);

        const response = await fetch(`${API_BASE_URL}/admin/order/update-status`, {
            method: 'POST',
            body: formData
        });

        const data = await response.json();
        Notiflix.Loading.remove();

        if (data.status) {
            Notiflix.Notify.success(`Order #${orderId} updated to ${newStatus}`);
            // Update select color immediately based on new status
            let statusColorClass = 'text-white';
            if (newStatus === 'PENDING') statusColorClass = 'text-yellow-500 border-yellow-500/50';
            else if (newStatus === 'COMPLETED' || newStatus === 'DELIVERED') statusColorClass = 'text-emerald-500 border-emerald-500/50';
            else if (newStatus === 'CANCELED') statusColorClass = 'text-red-500 border-red-500/50';
            else if (newStatus === 'PACKING') statusColorClass = 'text-blue-500 border-blue-500/50';

            // Remove old color classes and add new ones (simplistic replace)
            selectElement.className = `bg-black/20 border border-white/10 ${statusColorClass} text-xs rounded px-2 py-1 focus:outline-none focus:border-nike-green cursor-pointer`;

        } else {
            Notiflix.Notify.failure(data.message);
            // Revert select? (fetching list simplifies this)
            loadOrders(currentPage);
        }
    } catch (e) {
        Notiflix.Loading.remove();
        console.error("Error updating status", e);
        Notiflix.Notify.failure("Failed to update status");
    }
}

function renderPagination(totalPages, currentPage) {
    const container = document.getElementById('pagination-container');
    if (!container) return;

    container.innerHTML = '';
    if (totalPages <= 1) return;

    let html = '<div class="flex items-center gap-2 bg-black/40 p-1 rounded-lg border border-white/5">';

    // Prev
    const prevDisabled = currentPage === 1;
    html += `<button onclick="loadOrders(${currentPage - 1})" ${prevDisabled ? 'disabled' : ''} class="h-8 w-8 flex items-center justify-center rounded-md transition-all ${prevDisabled ? 'text-zinc-600 cursor-not-allowed' : 'text-zinc-400 hover:text-white hover:bg-white/10'}"><i class="fas fa-chevron-left text-xs"></i></button>`;

    // Page Numbers (Simplified logic: show all or simple range)
    for (let i = 1; i <= totalPages; i++) {
        const activeClass = i === currentPage ? 'bg-nike-green text-black font-bold shadow-lg shadow-nike-green/20' : 'text-zinc-400 hover:text-white hover:bg-white/10';
        html += `<button onclick="loadOrders(${i})" class="h-8 w-8 flex items-center justify-center rounded-md text-xs transition-all ${activeClass}">${i}</button>`;
    }

    // Next
    const nextDisabled = currentPage === totalPages;
    html += `<button onclick="loadOrders(${currentPage + 1})" ${nextDisabled ? 'disabled' : ''} class="h-8 w-8 flex items-center justify-center rounded-md transition-all ${nextDisabled ? 'text-zinc-600 cursor-not-allowed' : 'text-zinc-400 hover:text-white hover:bg-white/10'}"><i class="fas fa-chevron-right text-xs"></i></button>`;

    html += '</div>';
    container.innerHTML = html;
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-LK', {
        style: 'currency',
        currency: 'LKR',
        minimumFractionDigits: 2
    }).format(amount);
}
