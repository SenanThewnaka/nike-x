const API_BASE_URL = '../api';

document.addEventListener('DOMContentLoaded', () => {
    loadOrders();
});

async function loadOrders() {
    const container = document.getElementById('orders-container');

    try {
        const response = await fetch(`${API_BASE_URL}/order/list`);
        const data = await response.json();

        if (response.status === 401 || (data.status === false && data.message === "Please login")) {
            window.location.href = '../sign-in.html';
            return;
        }

        if (data.status && data.orders && data.orders.length > 0) {
            renderOrders(data.orders, container);
        } else {
            container.innerHTML = `
                <div class="glass-panel p-12 rounded-3xl text-center border border-white/10">
                    <div class="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-4">
                        <i class="fas fa-shopping-bag text-2xl text-gray-600"></i>
                    </div>
                    <h3 class="text-xl font-bold mb-2">No orders yet</h3>
                    <p class="text-gray-400 mb-6">Looks like you haven't bought anything yet.</p>
                    <a href="../index.html" class="inline-block bg-green-600 hover:bg-green-700 text-white font-bold py-3 px-8 rounded-xl transition-all">
                        Start Shopping
                    </a>
                </div>
            `;
        }

    } catch (e) {
        console.error("Failed to load orders", e);
        container.innerHTML = '<div class="text-center text-red-500 py-10">Failed to load order history.</div>';
    }
}

function renderOrders(orders, container) {
    container.innerHTML = orders.map(order => {

        let statusClass = '';
        const s = order.status.toUpperCase();
        if (s === 'PENDING') statusClass = 'text-yellow-500 bg-yellow-500/10 border-yellow-500/20';
        else if (s === 'CANCELED') statusClass = 'text-red-500 bg-red-500/10 border-red-500/20';
        else statusClass = 'text-green-500 bg-green-500/10 border-green-500/20';

        // Forcing UI consistency with Invoice (Green/Paid)
        const displayStatus = 'Paid';
        const displayClass = 'text-green-500 bg-green-500/10 border-green-500/20';


        // Items HTML
        let itemsHtml = '';
        if (order.items && order.items.length > 0) {
            itemsHtml = `<div class="w-full space-y-4 pt-4 border-t border-white/5">` +
                order.items.map(item => `
                    <div class="flex items-center gap-4 bg-white/5 p-3 rounded-xl">
                        <div class="w-16 h-16 rounded-lg bg-white/5 overflow-hidden flex-shrink-0">
                            <img src="../${item.imagePath}" alt="${item.productName}" class="w-full h-full object-cover">
                        </div>
                        <div class="flex-1">
                            <h4 class="font-bold text-sm">${item.productName}</h4>
                            <p class="text-xs text-gray-400">${item.variance}</p>
                            <div class="flex items-center gap-4 mt-1">
                                <span class="text-xs text-green-500 font-mono">Rs. ${item.price.toFixed(2)}</span>
                                <span class="text-xs text-gray-500">x${item.qty}</span>
                            </div>
                        </div>
                        <div class="font-bold text-sm">
                            Rs. ${item.total.toFixed(2)}
                        </div>
                    </div>
                `).join('') +
                `</div>`;
        }

        return `
            <div class="glass-panel p-6 rounded-3xl space-y-4 border border-white/5 hover:border-green-500/30 transition-all group z-10 relative">
                
                <div class="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                    <div class="flex items-center gap-4">
                        <div class="w-12 h-12 rounded-xl bg-green-500/20 flex items-center justify-center text-green-500">
                            <i class="fas fa-box"></i>
                        </div>
                        <div>
                            <h3 class="font-bold text-lg">Order #${order.id}</h3>
                            <p class="text-sm text-gray-400">${order.date}</p>
                        </div>
                    </div>

                    <div class="flex items-center gap-4 self-end md:self-auto">
                        <div class="px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider border ${displayClass}">
                            ${displayStatus}
                        </div>
                        <a href="invoice.html?id=${order.id}" 
                            class="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 transition-all text-sm font-bold flex items-center gap-2">
                            Invoice <i class="fas fa-arrow-right"></i>
                        </a>
                    </div>
                </div>

                ${itemsHtml}

                <div class="flex justify-between items-center pt-4 border-t border-white/10">
                    <span class="text-sm text-gray-400">Total Amount</span>
                    <span class="text-xl font-bold text-green-400">Rs. ${order.totalAmount.toFixed(2)}</span>
                </div>

            </div>
        `;
    }).join('');
}
