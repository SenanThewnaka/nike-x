const API_BASE_URL = 'api';

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const orderId = urlParams.get('id');

    if (orderId) {
        loadInvoice(orderId);
    } else {
        document.body.innerHTML = '<div class="text-center text-red-500 mt-20">Invalid Invoice URL</div>';
        setTimeout(() => window.location.href = 'index.html', 3000);
    }
});

async function loadInvoice(id) {
    Notiflix.Loading.pulse('Generating Invoice...');
    try {
        const response = await fetch(`../api/order/details?id=${id}`);
        const data = await response.json();

        if (data.status && data.order) {
            renderInvoice(data.order);
        } else {
            Notiflix.Report.failure('Error', data.message || 'Invoice not found', 'Home', () => {
                window.location.href = 'index.html';
            });
        }
    } catch (e) {
        console.error(e);
        Notiflix.Notify.failure('Failed to load invoice');
    } finally {
        Notiflix.Loading.remove();
    }
}

function renderInvoice(order) {
    // Header Info
    // Header Info
    setText('invoice-id', `#${order.id}`);
    setText('invoice-date', `Date: ${order.date}`);

    // Status Display
    const statusEl = document.getElementById('order-status');
    if (statusEl) {
        statusEl.innerText = order.status;

        // Define styles logic
        let style = 'bg-gray-500/20 text-gray-500 border-gray-500/30'; // Default

        if (order.status.toLowerCase() === 'paid') {
            style = 'bg-green-500/20 text-green-500 border-green-500/30';
        } else if (order.status.toLowerCase() === 'pending') {
            style = 'bg-yellow-500/20 text-yellow-500 border-yellow-500/30';
        } else if (order.status.toLowerCase() === 'cancelled') {
            style = 'bg-red-500/20 text-red-500 border-red-500/30';
        }

        statusEl.className = `px-4 py-1 rounded-full text-xs font-bold uppercase tracking-wider border ${style}`;
    }

    // Customer Info
    setText('customer-name', order.customerName);
    setText('customer-address', `${order.addressLine1}, ${order.addressLine2}`);
    setText('customer-city', `${order.city}, ${order.postalCode}`);
    setText('customer-email', order.customerEmail);

    // Payment Info
    setText('payment-method', order.paymentMethod);

    // Items
    const itemsContainer = document.getElementById('invoice-items');
    if (itemsContainer) {
        if (order.items && order.items.length > 0) {
            itemsContainer.innerHTML = order.items.map(item => `
                <div class="grid grid-cols-1 md:grid-cols-12 gap-4 py-6 border-b border-white/5 items-center">
                    <div class="col-span-6">
                        <p class="font-bold text-white">${item.productName}</p>
                        <p class="text-xs text-gray-500">${item.variance}</p>
                    </div>
                    <div class="col-span-2 text-left md:text-center text-sm text-gray-300">
                        <span class="md:hidden text-gray-500 mr-2">Qty:</span>${item.qty}
                    </div>
                    <div class="col-span-2 text-left md:text-right text-sm text-gray-300">
                        <span class="md:hidden text-gray-500 mr-2">Price:</span>Rs. ${item.price.toFixed(2)}
                    </div>
                    <div class="col-span-2 text-left md:text-right font-bold text-white">
                        Rs. ${item.total.toFixed(2)}
                    </div>
                </div>
            `).join('');
        } else {
            itemsContainer.innerHTML = '<div class="py-8 text-center text-gray-500">No items found</div>';
        }
    }

    // Totals
    setText('invoice-subtotal', `Rs. ${order.totalAmount.toFixed(2)}`);
    setText('invoice-total', `Rs. ${order.totalAmount.toFixed(2)}`);
}

function setText(id, text) {
    const el = document.getElementById(id);
    if (el) el.innerText = text;
}
