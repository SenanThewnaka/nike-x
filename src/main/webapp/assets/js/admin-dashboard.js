const API_BASE_URL = '../api'; // Admin pages are in /admin/, so api is ../api

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardStats();
    loadRecentOrders();
});

async function loadDashboardStats() {
    try {
        const response = await fetch(`${API_BASE_URL}/admin/stats`);
        const data = await response.json();

        if (data.status) {
            // Animate Numbers? Simple text for now
            document.getElementById('total-revenue').innerText = formatCurrency(data.totalRevenue);
            document.getElementById('total-orders').innerText = data.totalOrders;
            document.getElementById('active-users').innerText = data.activeUsers;
            document.getElementById('conversion-rate').innerText = data.conversionRate + '%';
        } else {
            console.error(data.message);
        }
    } catch (e) {
        console.error("Error loading stats", e);
    }
}

async function loadRecentOrders() {
    const tbody = document.getElementById('recent-orders-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-gray-500">Loading...</td></tr>';

    try {
        // Fetch Page 1, Limit 5 (Handled by backend default 10, maybe we slice in frontend or update API to accept limit)
        // Current API getAllOrders(page, limit=10)
        const response = await fetch(`${API_BASE_URL}/admin/orders?page=1`);
        const data = await response.json();

        if (data.status) {
            const orders = data.orders.slice(0, 5); // Just show top 5 for "Recent"

            if (orders.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-gray-500">No recent orders found.</td></tr>';
                return;
            }

            tbody.innerHTML = orders.map(order => {
                let statusColor = 'bg-gray-500/10 text-gray-500 border-gray-500/20';
                const status = (order.status || 'Pending').toUpperCase();

                if (status === 'PENDING') statusColor = 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20';
                else if (status === 'COMPLETED' || status === 'DELIVERED') statusColor = 'bg-nike-green/10 text-nike-green border-nike-green/20';
                else if (status === 'CANCELLED' || status === 'CANCELED') statusColor = 'bg-red-500/10 text-red-500 border-red-500/20';
                else if (status === 'SHIPPED' || status === 'PACKING') statusColor = 'bg-blue-500/10 text-blue-500 border-blue-500/20';

                const productName = order.firstProductName || 'Unknown Product';
                const customerName = order.customerName || 'Guest';
                let imagePath = order.firstProductImage || '';

                if (imagePath && !imagePath.startsWith('../') && !imagePath.startsWith('http')) {
                    imagePath = '../' + imagePath;
                }

                return `
                <tr class="hover:bg-white/5 transition-colors cursor-pointer" onclick="window.location.href='orders.html'">
                    <td class="px-6 py-4 font-medium text-white">#${order.id}</td>
                    <td class="px-6 py-4 flex items-center gap-3">
                        <div class="w-8 h-8 rounded bg-zinc-800 flex items-center justify-center text-xs text-gray-500 overflow-hidden border border-white/10 shrink-0">
                           ${imagePath ? `<img src="${imagePath}" class="w-full h-full object-cover">` : `<i class="fas fa-box"></i>`}
                        </div>
                        <span class="truncate w-32" title="${productName}">${productName}</span>
                    </td>
                    <td class="px-6 py-4">${customerName}</td>
                    <td class="px-6 py-4">${order.date || 'N/A'}</td>
                    <td class="px-6 py-4 text-white">${formatCurrency(order.totalAmount || 0)}</td>
                    <td class="px-6 py-4">
                        <span class="px-2 py-1 rounded text-[10px] font-bold border ${statusColor}">${status}</span>
                    </td>
                </tr>
                `;
            }).join('');

        } else {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-red-500">${data.message || 'Failed to load orders'}</td></tr>`;
        }
    } catch (e) {
        console.error(e);
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-red-500">Error loading orders.</td></tr>';
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-LK', {
        style: 'currency',
        currency: 'LKR',
        minimumFractionDigits: 2
    }).format(amount);
}
