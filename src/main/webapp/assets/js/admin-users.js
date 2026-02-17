const API_BASE_URL = '../api';

document.addEventListener('DOMContentLoaded', () => {
    loadUsers();
});

async function loadUsers() {
    const tbody = document.getElementById('users-table-body');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-gray-500">Loading users...</td></tr>';

    try {
        const response = await fetch(`${API_BASE_URL}/admin/users`);
        const data = await response.json();

        if (data.status) {
            const users = data.users;

            if (users.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-gray-500">No users found.</td></tr>';
                return;
            }

            tbody.innerHTML = users.map(user => {
                let statusColor = 'bg-gray-500/10 text-gray-500 border-gray-500/20';
                const status = user.statusName || 'Unknown';

                if (status === 'Active') statusColor = 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20';
                else if (status === 'Inactive') statusColor = 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20';
                else if (status === 'Banned') statusColor = 'bg-red-500/10 text-red-500 border-red-500/20';

                // Role - For now hardcoded or basic detection. User entity usually has roles?
                // Assuming customer for now unless email implies admin (admin@nike-x.com)
                let role = 'CUSTOMER';
                let roleColor = 'bg-zinc-700/50 text-zinc-300 border-zinc-600';

                // Hacky role detection (Real way: send role from backend)
                if (user.email.includes('@nike-x.com')) {
                    role = 'ADMIN';
                    roleColor = 'bg-purple-500/10 text-purple-500 border-purple-500/20';
                }

                const statusUpper = status.toUpperCase();
                let actionBtn = '';

                if (statusUpper === 'BLOCKED') {
                    actionBtn = `
                        <button onclick="toggleUserStatus(${user.id}, 'UNBLOCK')" 
                            class="text-emerald-500 hover:text-emerald-400 p-2 text-xs border border-emerald-500/30 rounded bg-emerald-500/10 transition-colors">
                            Unblock
                        </button>`;
                } else if (statusUpper === 'VERIFIED' || statusUpper === 'ACTIVE') {
                    actionBtn = `
                        <button onclick="toggleUserStatus(${user.id}, 'BLOCK')" 
                            class="text-red-500 hover:text-red-400 p-2 text-xs border border-red-500/30 rounded bg-red-500/10 transition-colors">
                            Block
                        </button>`;
                } else {
                    actionBtn = `
                        <button onclick="toggleUserStatus(${user.id}, 'BLOCK')" 
                            class="text-zinc-500 hover:text-white p-2 text-xs border border-zinc-700 rounded bg-zinc-800 transition-colors">
                            Block
                        </button>`;
                }

                const initials = (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();

                return `
                <tr class="hover:bg-white/5 transition-colors">
                    <td class="px-6 py-4">
                        <div class="flex items-center gap-3">
                            <div class="w-8 h-8 rounded-full bg-zinc-800 flex items-center justify-center text-xs font-bold text-white border border-white/10">
                                ${initials}
                            </div>
                            <div>
                                <p class="text-white">${user.firstName} ${user.lastName}</p>
                                <p class="text-xs text-zinc-500">${user.email}</p>
                            </div>
                        </div>
                    </td>
                    <td class="px-6 py-4">
                        <span class="px-2 py-1 rounded text-[10px] font-bold border ${roleColor}">${role}</span>
                    </td>
                    <td class="px-6 py-4">
                        <span class="px-2 py-1 rounded text-[10px] font-bold border ${statusColor}">${status.toUpperCase()}</span>
                    </td>
                    <td class="px-6 py-4">${user.createdAt || '-'}</td>
                    <td class="px-6 py-4 text-right">
                        ${actionBtn}
                    </td>
                </tr>
                `;
            }).join('');
        } else {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4 text-red-500">${data.message}</td></tr>`;
        }
    } catch (e) {
        console.error("Error loading users", e);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-red-500">Error loading users.</td></tr>';
    }
}

async function toggleUserStatus(userId, action) {
    Notiflix.Confirm.show(
        'Confirm Action',
        `Are you sure you want to ${action.toLowerCase()} this user?`,
        'Yes',
        'No',
        async function () {
            try {
                const response = await fetch(`${API_BASE_URL}/admin/user/update-status`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    body: `userId=${userId}&action=${action}`
                });
                const data = await response.json();

                if (data.status) {
                    Notiflix.Notify.success(data.message);
                    loadUsers();
                } else {
                    Notiflix.Notify.failure(data.message);
                }
            } catch (error) {
                console.error('Error updating status:', error);
                Notiflix.Notify.failure('Failed to update status');
            }
        }
    );
}
