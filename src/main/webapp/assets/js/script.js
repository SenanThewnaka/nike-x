tailwind.config = {
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'sans-serif'],
                futuristic: ['Orbitron', 'sans-serif'],
                condensed: ['Oswald', 'sans-serif'],
            },
            colors: {
                'nike-green': '#39ff14', // Neon Green
                'nike-dark': '#0a0a0a',
                'nike-gray': '#1a1a1a',
            },
            animation: {
                'fade-in-up': 'fadeInUp 0.8s ease-out forwards',
                'slide-in-right': 'slideInRight 0.8s ease-out forwards',
                'zoom-slow': 'zoomSlow 20s linear infinite alternate',
                'lava-1': 'lava 15s infinite linear',
                'lava-2': 'lava 18s infinite linear 2s',
                'lava-3': 'lava 20s infinite linear 1s',
                'lava-4': 'lava 12s infinite linear 4s',
                'lava-5': 'lava 25s infinite linear',
            },
            keyframes: {
                lava: {
                    '0%': { transform: 'translateY(0) scale(1)', opacity: '0' },
                    '20%': { opacity: '1' },
                    '80%': { opacity: '1' },
                    '100%': { transform: 'translateY(-500px) scale(1.2)', opacity: '0' },
                },
                fadeInUp: {
                    '0%': { opacity: '0', transform: 'translateY(20px)' },
                    '100%': { opacity: '1', transform: 'translateY(0)' },
                },
                slideInRight: {
                    '0%': { opacity: '0', transform: 'translateX(20px)' },
                    '100%': { opacity: '1', transform: 'translateX(0)' },
                },
                zoomSlow: {
                    '0%': { transform: 'scale(1)' },
                    '100%': { transform: 'scale(1.1)' },
                }
            }
        }
    }
}

function togglePassword() {
    const passwordInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eye-icon');
    const eyeOffIcon = document.getElementById('eye-off-icon');

    if (passwordInput && eyeIcon && eyeOffIcon) {
        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            eyeIcon.classList.add('hidden');
            eyeOffIcon.classList.remove('hidden');
        } else {
            passwordInput.type = 'password';
            eyeIcon.classList.remove('hidden');
            eyeOffIcon.classList.add('hidden');
        }
    }
}

function toggleChangeEmail() {
    const verifySection = document.getElementById('verify-section');
    const changeEmailSection = document.getElementById('change-email-section');

    if (verifySection && changeEmailSection) {
        if (verifySection.classList.contains('hidden')) {
            // Show Verify, Hide Change Email
            verifySection.classList.remove('hidden');
            changeEmailSection.classList.add('hidden');
            changeEmailSection.classList.remove('animate-slide-in-right');
        } else {
            // Show Change Email, Hide Verify
            verifySection.classList.add('hidden');
            changeEmailSection.classList.remove('hidden');
            changeEmailSection.classList.add('animate-slide-in-right');
        }
    }
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (sidebar && overlay) {
        if (sidebar.classList.contains('-translate-x-full')) {
            // Open Sidebar
            sidebar.classList.remove('-translate-x-full');
            overlay.classList.remove('hidden');
        } else {
            // Close Sidebar
            sidebar.classList.add('-translate-x-full');
            overlay.classList.add('hidden');
        }
    }
}
