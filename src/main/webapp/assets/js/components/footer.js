function loadFooter(basePath = '') {
    if (basePath && !basePath.endsWith('/')) basePath += '/';
    const footerHTML = `
    <footer class="border-t border-white/10 pt-16 pb-8 bg-black">
        <div class="container mx-auto px-6">
            <div class="grid grid-cols-2 md:grid-cols-4 gap-8 mb-12">
                <div>
                    <h4 class="text-xl font-bold mb-6">NIKE-X</h4>
                    <p class="text-gray-500 text-sm">Innovating the future of athletic wear. Designed for performance, style, and sustainability.</p>
                </div>
                <div>
                    <h5 class="font-bold mb-4">Shop</h5>
                    <ul class="space-y-2 text-gray-500 text-sm">
                        <li><a href="#" class="hover:text-green-400 transition-colors">New Arrivals</a></li>
                        <li><a href="#" class="hover:text-green-400 transition-colors">Men</a></li>
                        <li><a href="#" class="hover:text-green-400 transition-colors">Women</a></li>
                        <li><a href="#" class="hover:text-green-400 transition-colors">Accessories</a></li>
                    </ul>
                </div>
                <div>
                    <h5 class="font-bold mb-4">Company</h5>
                    <ul class="space-y-2 text-gray-500 text-sm">
                        <li><a href="${basePath}index.html#about" class="hover:text-green-400 transition-colors">About Us</a></li>
                        <li><a href="#" class="hover:text-green-400 transition-colors">Careers</a></li>
                        <li><a href="#" class="hover:text-green-400 transition-colors">Press</a></li>
                        <li><a href="#" class="hover:text-green-400 transition-colors">Sustainability</a></li>
                    </ul>
                </div>
                <div>
                    <h5 class="font-bold mb-4">Connect</h5>
                    <div class="flex space-x-4">
                        <a href="#" class="w-10 h-10 rounded-full glass-morphism flex items-center justify-center hover:bg-white/10 transition-colors"><i class="fab fa-instagram"></i></a>
                        <a href="#" class="w-10 h-10 rounded-full glass-morphism flex items-center justify-center hover:bg-white/10 transition-colors"><i class="fab fa-twitter"></i></a>
                        <a href="#" class="w-10 h-10 rounded-full glass-morphism flex items-center justify-center hover:bg-white/10 transition-colors"><i class="fab fa-facebook-f"></i></a>
                    </div>
                </div>
            </div>

            <div class="border-t border-white/5 pt-8 text-center text-gray-600 text-sm">
                <p>&copy; 2024 Nike-X. All rights reserved.</p>
            </div>
        </div>
    </footer>
    `;

    document.getElementById('footer-placeholder').innerHTML = footerHTML;
}
