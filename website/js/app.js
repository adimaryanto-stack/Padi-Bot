/**
 * PadiBot Interactive 3D UI Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. Initialize Field Simulator
  const sim = new window.FieldSimulator('fieldCanvas');

  // Simulator Buttons
  const btnStart = document.getElementById('btnSimStart');
  const btnPause = document.getElementById('btnSimPause');
  const btnEstop = document.getElementById('btnSimEstop');
  const btnClear = document.getElementById('btnSimClear');
  const btnPresetRect = document.getElementById('btnPresetRect');
  const btnPresetContour = document.getElementById('btnPresetContour');
  const speedBtns = document.querySelectorAll('.sim-speed-btn');

  if (btnStart) btnStart.addEventListener('click', () => sim.startMission());
  if (btnPause) btnPause.addEventListener('click', () => sim.pauseMission());
  if (btnEstop) btnEstop.addEventListener('click', () => sim.emergencyStop());
  if (btnClear) btnClear.addEventListener('click', () => sim.clearField());

  if (btnPresetRect) {
    btnPresetRect.addEventListener('click', () => {
      sim.loadPreset(0);
      btnPresetRect.classList.add('bg-emerald-600', 'text-white', 'border-emerald-600', 'shadow-md');
      btnPresetRect.classList.remove('bg-white', 'text-slate-700', 'border-slate-300');
      btnPresetContour.classList.remove('bg-emerald-600', 'text-white', 'border-emerald-600', 'shadow-md');
      btnPresetContour.classList.add('bg-white', 'text-slate-700', 'border-slate-300');
    });
  }

  if (btnPresetContour) {
    btnPresetContour.addEventListener('click', () => {
      sim.loadPreset(1);
      btnPresetContour.classList.add('bg-emerald-600', 'text-white', 'border-emerald-600', 'shadow-md');
      btnPresetContour.classList.remove('bg-white', 'text-slate-700', 'border-slate-300');
      btnPresetRect.classList.remove('bg-emerald-600', 'text-white', 'border-emerald-600', 'shadow-md');
      btnPresetRect.classList.add('bg-white', 'text-slate-700', 'border-slate-300');
    });
  }

  const patternBtns = document.querySelectorAll('.pattern-select-btn');

  patternBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      patternBtns.forEach(b => {
        b.classList.remove('bg-emerald-600', 'text-white', 'border-emerald-600');
        b.classList.add('bg-white', 'text-slate-700', 'border-slate-300');
      });
      btn.classList.add('bg-emerald-600', 'text-white', 'border-emerald-600');
      btn.classList.remove('bg-white', 'text-slate-700', 'border-slate-300');
      const pattern = btn.getAttribute('data-pattern') || 'boustrophedon';
      sim.setPattern(pattern);
    });
  });

  speedBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      speedBtns.forEach(b => {
        b.classList.remove('bg-emerald-600', 'text-white', 'border-emerald-600');
        b.classList.add('bg-white', 'text-slate-700', 'border-slate-200');
      });
      btn.classList.add('bg-emerald-600', 'text-white', 'border-emerald-600');
      btn.classList.remove('bg-white', 'text-slate-700', 'border-slate-200');
      const speed = parseFloat(btn.getAttribute('data-speed') || '1');
      sim.setSpeedMultiplier(speed);
    });
  });

  // 2. Interactive 3D Card Tilt Engine
  const tiltCards = document.querySelectorAll('.tilt-card');
  tiltCards.forEach(card => {
    card.addEventListener('mousemove', (e) => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      const centerX = rect.width / 2;
      const centerY = rect.height / 2;

      // Calculate tilt angles (-10 deg to +10 deg)
      const rotateX = ((y - centerY) / centerY) * -9;
      const rotateY = ((x - centerX) / centerX) * 9;

      card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-4px)`;
    });

    card.addEventListener('mouseleave', () => {
      card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0px)';
    });
  });

  // 3. Interactive 3D Phone App Screen Showcase
  const phoneCards = document.querySelectorAll('.phone-card-3d');
  const mainScreenImg = document.getElementById('featuredScreenImg');
  const mainScreenTitle = document.getElementById('featuredScreenTitle');
  const mainScreenBadge = document.getElementById('featuredScreenBadge');
  const mainScreenDesc = document.getElementById('featuredScreenDesc');
  const mainScreenSpecs = document.getElementById('featuredScreenSpecs');

  const screenDetails = {
    'dashboard': {
      title: 'Dashboard Kontrol Utama',
      badge: 'Layar 1 • Status Real-Time',
      img: 'assets/screens/dashboard.png',
      desc: 'Menampilkan status telemetri robot secara komprehensif: persentase baterai 82%, kualitas sinyal GPS RTK, kecepatan bergerak 6.0 m/s, serta mode penanaman AUTO. Dilengkapi tombol cepat untuk mulai pemetaan, memilih sawah, dan kontrol manual.',
      bullets: ['Visual indikator status koneksi real-time', 'Speedometer & Battery gauge', 'Akses cepat ke 3 mode operasional']
    },
    'mapping': {
      title: 'Pemetaan Batas Sawah Presisi',
      badge: 'Layar 2 • Boundary GPS',
      img: 'assets/screens/mapping.png',
      desc: 'Petani menandai titik pematang sawah langsung di peta satelit. Sistem mencatat jarak batas keliling (120 m) dengan akurasi sentimeter (3 cm) dan membentuk poligon sawah otomatis.',
      bullets: ['Mode perekaman titik demi titik (Point & Polygon)', 'Akurasi pembacaan GPS RTK hingga 3 cm', 'Penghitung jarak keliling & luas otomatis']
    },
    'settings': {
      title: 'Pengaturan Parameter Tanam',
      badge: 'Layar 3 • Konfigurasi Agronomi',
      img: 'assets/screens/settings.png',
      desc: 'Kustomisasi jarak tanam antar-baris (30 cm), jarak antar-tanaman (20 cm), lebar efektif kerja mesin (120 cm), kecepatan operasi (0.8 m/s), serta jarak putar headland (1.5 m).',
      bullets: ['Pengaturan jarak tanam Jajar Legowo & Konvensional', 'Pengaturan kecepatan dorong & kedalaman tanam', 'Tombol instan Generate Jalur Tanam']
    },
    'path_gen': {
      title: 'Kalkulasi Jalur Otomatis (Boustrophedon)',
      badge: 'Layar 4 • Path Planning',
      img: 'assets/screens/path_gen.png',
      desc: 'Algoritma cerdas menghitung rute zig-zag bolak-balik (lawnmower) di dalam batas sawah, lengkap dengan zona putar headland dan penanda titik awal (Start) & akhir (End).',
      bullets: ['Optimasi rute minim lintasan tumpang tindih', 'Visualisasi zona putar traktor di tepi sawah', 'Estimasi waktu dan bibit yang dibutuhkan']
    },
    'auto_mode': {
      title: 'Eksekusi Misi Otonom',
      badge: 'Layar 5 • Live Monitoring',
      img: 'assets/screens/auto_mode.png',
      desc: 'Pantau pergerakan robot secara real-time saat menanam padi di sawah. Menampilkan persentase progres (72%), luas area yang selesai (1.240 m²), sisa area (480 m²), dan tombol Pause/Stop darurat.',
      bullets: ['Live tracking posisi traktor di atas peta', 'Real-time calculation luas selesai vs sisa', 'Kontrol Start, Pause, Resume, dan Emergency Stop']
    },
    'report': {
      title: 'Riwayat & Laporan Hasil Tanam',
      badge: 'Layar 6 • Laporan Lengkap',
      img: 'assets/screens/report.png',
      desc: 'Log historis setiap misi tanam tersimpan di SQLite lokal. Menyajikan laporan luas total lahan (2.000 m²), area tertanam sukses (1.950 m²), area terlewat (50 m²), dan durasi pengerjaan (1 jam 32 menit).',
      bullets: ['Laporan detail efisiensi cakupan lahan', 'Peta jejak historis penanaman lengkap', 'Tersimpan 100% offline di memori smartphone']
    }
  };

  phoneCards.forEach(card => {
    card.addEventListener('click', () => {
      const key = card.getAttribute('data-screen-key');
      const data = screenDetails[key];
      if (!data) return;

      phoneCards.forEach(c => c.classList.remove('phone-card-active', 'ring-4', 'ring-emerald-500'));
      card.classList.add('phone-card-active', 'ring-4', 'ring-emerald-500');

      if (mainScreenImg) {
        mainScreenImg.src = data.img;
        mainScreenImg.classList.add('scale-95');
        setTimeout(() => mainScreenImg.classList.remove('scale-95'), 200);
      }
      if (mainScreenTitle) mainScreenTitle.textContent = data.title;
      if (mainScreenBadge) mainScreenBadge.textContent = data.badge;
      if (mainScreenDesc) mainScreenDesc.textContent = data.desc;

      if (mainScreenSpecs) {
        mainScreenSpecs.innerHTML = data.bullets.map(b => `
          <li class="flex items-center space-x-2 text-xs font-mono text-slate-700">
            <span class="w-2 h-2 rounded-full bg-emerald-500 flex-shrink-0"></span>
            <span>${b}</span>
          </li>
        `).join('');
      }
    });
  });

  // 4. Interactive 3D Schematic Hotspots Inspector
  const hotspotData = {
    'gps': {
      title: 'Antena GNSS RTK Multi-Konstelasi',
      badge: 'Centimeter Precision Navigation',
      desc: 'Terpasang pada tiang atas robot untuk menerima sinyal satelit GPS, GLONASS, Galileo, dan BeiDou dengan akurasi posisi tanam hingga ±2.5 cm di area sawah terbuka.',
      specs: ['Up to 10 Hz Update Rate', 'High Sensitivity -167 dBm', 'Active Ceramic Patch Antenna', 'Dukungan Koreksi RTCM 3.X']
    },
    'mcu': {
      title: 'Panel Kontrol Arduino Mega 2560 & ESP32',
      badge: 'Main Processing Unit',
      desc: 'Otak utama robot yang memproses navigasi otonom, kendali motor driver, encoder roda traktor, dan telemetri nirkabel ke smartphone.',
      specs: ['16 MHz ATmega2560 Master Core', 'Dual-Core 240MHz ESP32 WiFi/BLE', '54 Digital I/O (15 PWM pins)', '4 Hardware UART Serial Ports']
    },
    'estop': {
      title: 'Emergency Stop Fisik (Hardwired)',
      badge: 'Fail-Safe Safety System',
      desc: 'Tombol merah darurat fisik di bagian atas mesin yang langsung memutus aliran daya motor penggerak dalam tempo <15 milidetik saat ditekan.',
      specs: ['Direct Cut-Off Power Relay', 'Tahan percikan air & lumpur sawah IP65', 'Terhubung ke software alarm Android']
    },
    'planter': {
      title: 'Mekanisme Tanam & Tray Bibit Padi',
      badge: 'Automated Seedling Feeder',
      desc: 'Baki penampung bibit dengan lengan capit rotary otomatis yang menancapkan bibit pada kedalaman 2-4 cm sesuai jarak jajar legowo yang diatur.',
      specs: ['4-Row Simultaneous Planting', 'Depth Setting: 2-5 cm Presisi', 'Spacing: 15-30 cm Auto-adjust', 'Sensor Anti-Macet Bibit']
    },
    'motor': {
      title: 'Motor Penggerak Track & Kemudi IBT-2',
      badge: 'Heavy-Duty Mud Traversing',
      desc: 'Ditenagai motor DC bertorsi tinggi dengan driver H-Bridge IBT-2 (43A per channel), mampu melintasi lumpur sawah dalam tanpa selip.',
      specs: ['Dual IBT-2 43A High-Current Driver', 'High-Torque Planetary Gearbox', 'Track Karet Anti-Selip Lumpur', 'Kemudi Diferensial Presisi']
    }
  };

  const hotspotBtns = document.querySelectorAll('.hotspot-btn');
  const inspectorTitle = document.getElementById('inspectorTitle');
  const inspectorBadge = document.getElementById('inspectorBadge');
  const inspectorDesc = document.getElementById('inspectorDesc');
  const inspectorSpecs = document.getElementById('inspectorSpecs');

  hotspotBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const key = btn.getAttribute('data-hotspot');
      const data = hotspotData[key];
      if (!data) return;

      hotspotBtns.forEach(b => {
        b.classList.remove('ring-4', 'ring-emerald-400', 'bg-emerald-600');
        b.classList.add('bg-emerald-500');
      });
      btn.classList.add('ring-4', 'ring-emerald-400', 'bg-emerald-600');

      if (inspectorTitle) inspectorTitle.textContent = data.title;
      if (inspectorBadge) inspectorBadge.textContent = data.badge;
      if (inspectorDesc) inspectorDesc.textContent = data.desc;
      
      if (inspectorSpecs) {
        inspectorSpecs.innerHTML = data.specs.map(s => `
          <li class="flex items-center space-x-2 text-xs font-mono text-slate-700">
            <span class="w-2 h-2 rounded-full bg-emerald-500 flex-shrink-0"></span>
            <span>${s}</span>
          </li>
        `).join('');
      }
    });
  });

  // 5. Live Telemetry Random Fluctuations
  setInterval(() => {
    const battVoltsEl = document.getElementById('telemBattVolt');
    const motorTempEl = document.getElementById('telemMotorTemp');
    const pingEl = document.getElementById('telemPing');

    if (battVoltsEl) {
      const v = (24.6 + Math.random() * 0.4).toFixed(1);
      battVoltsEl.textContent = `${v}V`;
    }
    if (motorTempEl) {
      const t = (32 + Math.random() * 2).toFixed(1);
      motorTempEl.textContent = `${t}°C`;
    }
    if (pingEl) {
      const p = Math.floor(12 + Math.random() * 6);
      pingEl.textContent = `${p}ms`;
    }
  }, 2500);

  // 6. FAQ Accordion
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const header = item.querySelector('.faq-header');
    const content = item.querySelector('.faq-content');
    const icon = item.querySelector('.faq-icon');

    if (header && content) {
      header.addEventListener('click', () => {
        const isOpen = !content.classList.contains('hidden');
        faqItems.forEach(other => {
          other.querySelector('.faq-content')?.classList.add('hidden');
          other.querySelector('.faq-icon')?.classList.remove('rotate-180');
        });

        if (!isOpen) {
          content.classList.remove('hidden');
          if (icon) icon.classList.add('rotate-180');
        }
      });
    }
  });

  // 7. Toast Notification Handler for Downloads
  const downloadBtns = document.querySelectorAll('.download-trigger-btn');
  const toast = document.getElementById('toastNotification');

  downloadBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      if (toast) {
        toast.classList.remove('translate-y-24', 'opacity-0');
        toast.classList.add('translate-y-0', 'opacity-100');
        setTimeout(() => {
          toast.classList.add('translate-y-24', 'opacity-0');
          toast.classList.remove('translate-y-0', 'opacity-100');
        }, 4000);
      }
    });
  });

  // 8. Mobile Navigation Menu
  const mobileMenuBtn = document.getElementById('mobileMenuBtn');
  const mobileMenu = document.getElementById('mobileMenu');
  if (mobileMenuBtn && mobileMenu) {
    mobileMenuBtn.addEventListener('click', () => {
      mobileMenu.classList.toggle('hidden');
    });
  }

  // 9. Scroll-to-Top Circular Progress Indicator (adigue style)
  const scrollTopBtn = document.getElementById('scrollTopBtn');
  const scrollProgressBar = document.getElementById('scrollProgressBar');
  const circumference = 2 * Math.PI * 22;

  if (scrollTopBtn && scrollProgressBar) {
    scrollProgressBar.style.strokeDasharray = circumference;
    scrollProgressBar.style.strokeDashoffset = circumference;

    window.addEventListener('scroll', () => {
      const scrollY = window.scrollY || window.pageYOffset;
      const docHeight = document.documentElement.scrollHeight - window.innerHeight;
      const scrollFraction = docHeight > 0 ? scrollY / docHeight : 0;

      if (scrollY > 280) {
        scrollTopBtn.classList.add('active');
      } else {
        scrollTopBtn.classList.remove('active');
      }

      const offset = circumference - (scrollFraction * circumference);
      scrollProgressBar.style.strokeDashoffset = offset;
    }, { passive: true });

    scrollTopBtn.addEventListener('click', () => {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    });
  }

  // 10. Precision Navbar Active State Scrollspy
  const sectionIds = ['overview', 'app-showcase', 'simulator', 'schematics', 'patterns', 'faq'];
  const desktopNavLinks = document.querySelectorAll('.nav-link');
  const mobileNavLinks = document.querySelectorAll('.mobile-nav-link');

  function updateActiveNavLink() {
    const scrollPosition = window.scrollY + 160; // offset for navbar
    let currentSectionId = 'overview';

    if (window.scrollY < 200) {
      currentSectionId = 'overview';
    } else {
      sectionIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
          const top = el.offsetTop;
          const height = el.offsetHeight;
          if (scrollPosition >= top && scrollPosition < top + height) {
            currentSectionId = id;
          }
        }
      });

      // If scrolled to bottom of document, highlight FAQ
      if ((window.innerHeight + window.scrollY) >= (document.documentElement.scrollHeight - 60)) {
        currentSectionId = 'faq';
      }
    }

    // Update Desktop Nav Links
    desktopNavLinks.forEach(link => {
      if (link.getAttribute('data-section') === currentSectionId) {
        link.classList.add('nav-link-active');
        link.classList.remove('nav-link-inactive');
      } else {
        link.classList.remove('nav-link-active');
        link.classList.add('nav-link-inactive');
      }
    });

    // Update Mobile Nav Links
    mobileNavLinks.forEach(link => {
      if (link.getAttribute('data-section') === currentSectionId) {
        link.classList.add('nav-link-active');
        link.classList.remove('nav-link-inactive');
      } else {
        link.classList.remove('nav-link-active');
        link.classList.add('nav-link-inactive');
      }
    });
  }

  window.addEventListener('scroll', updateActiveNavLink, { passive: true });
  updateActiveNavLink(); // Run on initial page load
});
