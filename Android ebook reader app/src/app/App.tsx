import { useState, useRef } from "react";
import {
  Home, BookOpen, Search, Bell, User, ArrowLeft, MoreVertical,
  Sun, Moon, Minus, Plus, Bookmark, MessageSquare, Share2,
  ChevronRight, Star, Download, Upload, Filter, Grid, List,
  Highlighter, StickyNote, Trash2, Eye, Clock, CheckCircle,
  Heart, X, Menu, Settings, LogOut, Edit3, FileText,
  Smartphone, FolderOpen, PenLine, ChevronDown, Play, Layers
} from "lucide-react";

type Screen =
  | "login"
  | "register"
  | "home"
  | "library"
  | "reader"
  | "notes"
  | "book-detail"
  | "search"
  | "profile";

const BOOKS = [
  {
    id: 1,
    title: "Atomic Habits",
    author: "James Clear",
    cover: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=200&h=300&fit=crop&auto=format",
    coverBg: "#2D2416",
    progress: 68,
    pages: 320,
    currentPage: 218,
    genre: "Self-Help",
    rating: 4.8,
    description: "An easy and proven way to build good habits and break bad ones.",
    highlight: "#E8A44A",
    status: "reading",
    local: false,
  },
  {
    id: 2,
    title: "The Midnight Library",
    author: "Matt Haig",
    cover: "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=200&h=300&fit=crop&auto=format",
    coverBg: "#12202D",
    progress: 34,
    pages: 288,
    currentPage: 98,
    genre: "Fiction",
    rating: 4.5,
    description: "Between life and death there is a library, and within that library, the shelves go on forever.",
    highlight: "#5B8FD4",
    status: "reading",
    local: false,
  },
  {
    id: 3,
    title: "Sapiens",
    author: "Yuval Noah Harari",
    cover: "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=200&h=300&fit=crop&auto=format",
    coverBg: "#1D1D12",
    progress: 100,
    pages: 443,
    currentPage: 443,
    genre: "History",
    rating: 4.7,
    description: "A brief history of humankind.",
    highlight: "#7EC8A0",
    status: "finished",
    local: false,
  },
  {
    id: 4,
    title: "Thinking, Fast and Slow",
    author: "Daniel Kahneman",
    cover: "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=200&h=300&fit=crop&auto=format",
    coverBg: "#1A1225",
    progress: 0,
    pages: 499,
    currentPage: 0,
    genre: "Psychology",
    rating: 4.6,
    description: "Explores the two systems that drive the way we think.",
    highlight: "#B07ED4",
    status: "want",
    local: false,
  },
  {
    id: 5,
    title: "Deep Work",
    author: "Cal Newport",
    cover: "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=200&h=300&fit=crop&auto=format",
    coverBg: "#1A1212",
    progress: 55,
    pages: 296,
    currentPage: 163,
    genre: "Productivity",
    rating: 4.4,
    description: "Rules for focused success in a distracted world.",
    highlight: "#D46B6B",
    status: "reading",
    local: true,
  },
  {
    id: 6,
    title: "Tôi Thấy Hoa Vàng Trên Cỏ Xanh",
    author: "Nguyễn Nhật Ánh",
    cover: "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=200&h=300&fit=crop&auto=format",
    coverBg: "#1A1D12",
    progress: 80,
    pages: 248,
    currentPage: 198,
    genre: "Fiction",
    rating: 4.9,
    description: "Câu chuyện tuổi thơ trong sáng, đẹp đẽ.",
    highlight: "#8FD47E",
    status: "reading",
    local: true,
  },
];

const PUBLIC_BOOKS = [
  {
    id: 101,
    title: "Project Gutenberg Classics",
    author: "Various Authors",
    cover: "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=200&h=300&fit=crop&auto=format",
    coverBg: "#201A12",
    genre: "Classics",
    rating: 4.3,
    size: "2.4 MB",
  },
  {
    id: 102,
    title: "The Great Gatsby",
    author: "F. Scott Fitzgerald",
    cover: "https://images.unsplash.com/photo-1519682337058-a94d519337bc?w=200&h=300&fit=crop&auto=format",
    coverBg: "#121620",
    genre: "Classic",
    rating: 4.5,
    size: "1.1 MB",
  },
  {
    id: 103,
    title: "Pride and Prejudice",
    author: "Jane Austen",
    cover: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=200&h=300&fit=crop&auto=format",
    coverBg: "#201218",
    genre: "Romance",
    rating: 4.7,
    size: "0.9 MB",
  },
  {
    id: 104,
    title: "Moby Dick",
    author: "Herman Melville",
    cover: "https://images.unsplash.com/photo-1472214103451-9374bd1c798e?w=200&h=300&fit=crop&auto=format",
    coverBg: "#101820",
    genre: "Adventure",
    rating: 4.1,
    size: "1.8 MB",
  },
];

const HIGHLIGHTS = [
  {
    id: 1,
    bookId: 1,
    bookTitle: "Atomic Habits",
    color: "#E8A44A",
    text: "You do not rise to the level of your goals. You fall to the level of your systems.",
    note: "Core insight of the book — systems over goals.",
    page: 27,
    date: "2 days ago",
  },
  {
    id: 2,
    bookId: 1,
    bookTitle: "Atomic Habits",
    color: "#5B8FD4",
    text: "Every action you take is a vote for the type of person you wish to become.",
    note: "",
    page: 38,
    date: "2 days ago",
  },
  {
    id: 3,
    bookId: 2,
    bookTitle: "The Midnight Library",
    color: "#7EC8A0",
    text: "She read the way drowning people grab for a life preserver.",
    note: "Beautiful metaphor.",
    page: 52,
    date: "5 days ago",
  },
  {
    id: 4,
    bookId: 2,
    bookTitle: "The Midnight Library",
    color: "#E8A44A",
    text: "The only way to learn is to live.",
    note: "",
    page: 89,
    date: "5 days ago",
  },
  {
    id: 5,
    bookId: 3,
    bookTitle: "Sapiens",
    color: "#B07ED4",
    text: "Culture tends to argue that it forbids only that which is unnatural. But from a biological perspective, nothing is unnatural.",
    note: "Chapter 11 — a very provocative idea.",
    page: 167,
    date: "2 weeks ago",
  },
];

const READER_TEXT = `Chapter 7: The 2-Minute Rule

When you start a new habit, it should take less than two minutes to do.

You'll find that nearly any habit can be scaled down into a two-minute version: "Read before bed each night" becomes "Read one page." "Do thirty minutes of yoga" becomes "Take out my yoga mat." "Study for class" becomes "Open my notes."

The idea is to make your habits as easy as possible to start. Anyone can meditate for one minute, read one page, or put one item of clothing away. And, as we've just discussed, this is a powerful strategy because once you've started doing the right thing, it is much easier to continue doing it.

A new habit should not feel like a challenge. The actions that follow can be challenging, but the first two minutes should be easy. What you want is a "gateway habit" that naturally leads you down a more productive path.

The more you ritualize the beginning of a process, the more likely it becomes that you can slip into the state of deep focus that is required to do great things. By doing the same warm-up before every workout, you make it easier to get into a state of peak performance. By following the same creative ritual before every work session, you make it easier to get into the state of deep focus.`;

// ————— UI Components —————

function ProgressRing({ progress, size = 44, stroke = 3, color = "#E8A44A" }: {
  progress: number; size?: number; stroke?: number; color?: string;
}) {
  const r = (size - stroke * 2) / 2;
  const c = 2 * Math.PI * r;
  const dash = (progress / 100) * c;
  return (
    <svg width={size} height={size} style={{ transform: "rotate(-90deg)" }}>
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth={stroke} />
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth={stroke}
        strokeDasharray={`${dash} ${c}`} strokeLinecap="round" />
    </svg>
  );
}

function BookCoverCard({ book, onPress, size = "md" }: {
  book: typeof BOOKS[0]; onPress: () => void; size?: "sm" | "md" | "lg";
}) {
  const dims = size === "sm" ? "w-20 h-28" : size === "lg" ? "w-36 h-52" : "w-28 h-40";
  return (
    <button onClick={onPress} className="relative flex-shrink-0 rounded-lg overflow-hidden group" style={{ background: book.coverBg }}>
      <img src={book.cover} alt={book.title} className={`${dims} object-cover opacity-90 group-active:opacity-70 transition-opacity`} />
      <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
    </button>
  );
}

function BottomNav({ active, onNav }: { active: Screen; onNav: (s: Screen) => void }) {
  const tabs = [
    { id: "home" as Screen, icon: Home, label: "Home" },
    { id: "search" as Screen, icon: Search, label: "Explore" },
    { id: "library" as Screen, icon: BookOpen, label: "Library" },
    { id: "notes" as Screen, icon: Highlighter, label: "Notes" },
    { id: "profile" as Screen, icon: User, label: "Profile" },
  ];
  return (
    <nav className="flex items-center justify-around border-t px-1 py-2 flex-shrink-0" style={{ background: "#0E0E0F", borderColor: "rgba(255,255,255,0.07)" }}>
      {tabs.map(({ id, icon: Icon, label }) => {
        const isActive = active === id;
        return (
          <button key={id} onClick={() => onNav(id)} className="flex flex-col items-center gap-0.5 px-3 py-1 rounded-lg transition-all">
            <Icon size={22} className={isActive ? "text-primary" : "text-muted-foreground"} style={{ color: isActive ? "#E8A44A" : "#8A8A8E" }} />
            <span className="text-[10px] font-medium" style={{ color: isActive ? "#E8A44A" : "#8A8A8E" }}>{label}</span>
          </button>
        );
      })}
    </nav>
  );
}

// ————— Screens —————

function LoginScreen({ onNav }: { onNav: (s: Screen) => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <div className="flex flex-col h-full px-6" style={{ background: "#0E0E0F" }}>
      <div className="flex-1 flex flex-col justify-center gap-8">
        {/* Logo */}
        <div className="flex flex-col items-center gap-3">
          <div className="w-14 h-14 rounded-2xl flex items-center justify-center" style={{ background: "rgba(232,164,74,0.15)", border: "1px solid rgba(232,164,74,0.3)" }}>
            <BookOpen size={28} style={{ color: "#E8A44A" }} />
          </div>
          <div className="text-center">
            <h1 className="text-2xl font-bold tracking-tight" style={{ fontFamily: "'Plus Jakarta Sans', sans-serif", color: "#F0EBE0" }}>
              Folio
            </h1>
            <p className="text-sm mt-0.5" style={{ color: "#8A8A8E" }}>Your personal reading space</p>
          </div>
        </div>

        {/* Form */}
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium tracking-wide uppercase" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Email</label>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="you@example.com"
              className="w-full px-4 py-3 rounded-xl text-sm outline-none transition-all"
              style={{ background: "#1A1A1C", color: "#F0EBE0", border: "1px solid rgba(255,255,255,0.07)", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-medium tracking-wide uppercase" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Password</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-4 py-3 rounded-xl text-sm outline-none"
              style={{ background: "#1A1A1C", color: "#F0EBE0", border: "1px solid rgba(255,255,255,0.07)", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
            />
          </div>
          <div className="flex justify-end">
            <button className="text-xs" style={{ color: "#E8A44A", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Forgot password?</button>
          </div>
          <button
            onClick={() => onNav("home")}
            className="w-full py-3.5 rounded-xl text-sm font-semibold tracking-wide transition-all active:opacity-80"
            style={{ background: "#E8A44A", color: "#0E0E0F", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          >
            Sign In
          </button>
        </div>

        {/* Divider */}
        <div className="flex items-center gap-3">
          <div className="flex-1 h-px" style={{ background: "rgba(255,255,255,0.07)" }} />
          <span className="text-xs" style={{ color: "#8A8A8E" }}>or continue with</span>
          <div className="flex-1 h-px" style={{ background: "rgba(255,255,255,0.07)" }} />
        </div>

        {/* Social */}
        <div className="flex gap-3">
          {["Google", "Apple"].map(p => (
            <button key={p} className="flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-all active:opacity-70"
              style={{ background: "#1A1A1C", color: "#F0EBE0", border: "1px solid rgba(255,255,255,0.07)", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>
              {p === "Google" ? "G" : ""}
              {p}
            </button>
          ))}
        </div>
      </div>

      <div className="pb-8 text-center">
        <span className="text-sm" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>
          Don't have an account?{" "}
          <button onClick={() => onNav("register")} style={{ color: "#E8A44A" }} className="font-semibold">Sign up</button>
        </span>
      </div>
    </div>
  );
}

function RegisterScreen({ onNav }: { onNav: (s: Screen) => void }) {
  return (
    <div className="flex flex-col h-full px-6" style={{ background: "#0E0E0F" }}>
      {/* Header */}
      <div className="flex items-center pt-4 pb-2">
        <button onClick={() => onNav("login")} className="p-2 -ml-2">
          <ArrowLeft size={20} style={{ color: "#F0EBE0" }} />
        </button>
      </div>

      <div className="flex-1 flex flex-col justify-center gap-7">
        <div>
          <h1 className="text-2xl font-bold" style={{ fontFamily: "'Plus Jakarta Sans', sans-serif", color: "#F0EBE0" }}>Create account</h1>
          <p className="text-sm mt-1" style={{ color: "#8A8A8E" }}>Start your reading journey today</p>
        </div>

        <div className="flex flex-col gap-4">
          {[
            { label: "Full Name", type: "text", ph: "Your name" },
            { label: "Email", type: "email", ph: "you@example.com" },
            { label: "Password", type: "password", ph: "Min. 8 characters" },
            { label: "Confirm Password", type: "password", ph: "Repeat password" },
          ].map(f => (
            <div key={f.label} className="flex flex-col gap-1.5">
              <label className="text-xs font-medium tracking-wide uppercase" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{f.label}</label>
              <input type={f.type} placeholder={f.ph} className="w-full px-4 py-3 rounded-xl text-sm outline-none"
                style={{ background: "#1A1A1C", color: "#F0EBE0", border: "1px solid rgba(255,255,255,0.07)", fontFamily: "'Plus Jakarta Sans', sans-serif" }} />
            </div>
          ))}

          <div className="flex items-start gap-3 mt-1">
            <div className="w-5 h-5 rounded flex-shrink-0 mt-0.5 flex items-center justify-center" style={{ background: "#E8A44A" }}>
              <CheckCircle size={12} style={{ color: "#0E0E0F" }} />
            </div>
            <p className="text-xs leading-relaxed" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>
              I agree to the <span style={{ color: "#E8A44A" }}>Terms of Service</span> and <span style={{ color: "#E8A44A" }}>Privacy Policy</span>
            </p>
          </div>

          <button
            onClick={() => onNav("home")}
            className="w-full py-3.5 rounded-xl text-sm font-semibold tracking-wide mt-2"
            style={{ background: "#E8A44A", color: "#0E0E0F", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          >
            Create Account
          </button>
        </div>
      </div>

      <div className="pb-8 text-center">
        <span className="text-sm" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>
          Already have an account?{" "}
          <button onClick={() => onNav("login")} style={{ color: "#E8A44A" }} className="font-semibold">Sign in</button>
        </span>
      </div>
    </div>
  );
}

function HomeScreen({ onNav, onOpenBook }: { onNav: (s: Screen) => void; onOpenBook: (b: typeof BOOKS[0]) => void }) {
  const [activeGenre, setActiveGenre] = useState("All");
  const genres = ["All", "Fiction", "Self-Help", "History", "Psychology", "Science"];
  const reading = BOOKS.filter(b => b.status === "reading");

  return (
    <div className="flex flex-col h-full overflow-hidden" style={{ background: "#0E0E0F" }}>
      {/* Header */}
      <div className="flex items-center justify-between px-5 pt-4 pb-3 flex-shrink-0">
        <div>
          <p className="text-xs font-medium" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Good evening,</p>
          <h1 className="text-xl font-bold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Minh Tuấn</h1>
        </div>
        <div className="flex items-center gap-2">
          <button className="w-9 h-9 rounded-full flex items-center justify-center" style={{ background: "#1A1A1C" }}>
            <Bell size={18} style={{ color: "#F0EBE0" }} />
          </button>
          <button className="w-9 h-9 rounded-full overflow-hidden" style={{ background: "#242426" }}>
            <img src="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&h=80&fit=crop&auto=format" alt="avatar" className="w-full h-full object-cover" />
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
        {/* Continue Reading */}
        <div className="mb-6">
          <div className="flex items-center justify-between px-5 mb-3">
            <h2 className="text-base font-semibold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Continue Reading</h2>
            <button className="text-xs font-medium flex items-center gap-0.5" style={{ color: "#E8A44A", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>
              All <ChevronRight size={13} />
            </button>
          </div>

          {/* Featured book */}
          <div className="mx-5 rounded-2xl overflow-hidden relative h-44 mb-3" style={{ background: reading[0].coverBg }}>
            <img src={reading[0].cover} alt={reading[0].title} className="absolute inset-0 w-full h-full object-cover opacity-40" />
            <div className="absolute inset-0 bg-gradient-to-r from-black/80 via-black/40 to-transparent" />
            <div className="absolute inset-0 p-4 flex items-end">
              <div className="flex items-end gap-3 w-full">
                <button onClick={() => onOpenBook(reading[0])} className="active:opacity-70">
                  <img src={reading[0].cover} alt={reading[0].title} className="w-20 h-28 object-cover rounded-lg shadow-xl" />
                </button>
                <div className="flex-1 pb-1">
                  <p className="text-[10px] font-medium uppercase tracking-wider mb-1" style={{ color: "#E8A44A" }}>{reading[0].genre}</p>
                  <h3 className="text-base font-bold leading-tight mb-0.5" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{reading[0].title}</h3>
                  <p className="text-xs mb-3" style={{ color: "#8A8A8E" }}>{reading[0].author}</p>
                  <div className="flex items-center gap-2">
                    <div className="flex-1 h-1 rounded-full" style={{ background: "rgba(255,255,255,0.12)" }}>
                      <div className="h-full rounded-full" style={{ width: `${reading[0].progress}%`, background: "#E8A44A" }} />
                    </div>
                    <span className="text-[10px] font-medium" style={{ color: "#E8A44A" }}>{reading[0].progress}%</span>
                  </div>
                </div>
              </div>
            </div>
            <button
              onClick={() => onOpenBook(reading[0])}
              className="absolute top-3 right-3 px-3 py-1.5 rounded-full text-xs font-semibold flex items-center gap-1"
              style={{ background: "#E8A44A", color: "#0E0E0F", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
            >
              <Play size={10} /> Continue
            </button>
          </div>

          {/* Other reading books */}
          <div className="flex gap-3 pl-5 overflow-x-auto pb-1" style={{ scrollbarWidth: "none" }}>
            {reading.slice(1).map(book => (
              <button key={book.id} onClick={() => onOpenBook(book)} className="flex-shrink-0 active:opacity-70">
                <div className="relative">
                  <img src={book.cover} alt={book.title} className="w-20 h-28 object-cover rounded-lg" />
                  <div className="absolute -bottom-1 -right-1">
                    <ProgressRing progress={book.progress} size={24} stroke={2.5} color={book.highlight} />
                    <span className="absolute inset-0 flex items-center justify-center text-[7px] font-bold" style={{ color: "#F0EBE0" }}>{book.progress}%</span>
                  </div>
                </div>
                <p className="text-[10px] font-medium mt-2 w-20 truncate text-left" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</p>
              </button>
            ))}
          </div>
        </div>

        {/* Public Library */}
        <div className="mb-6">
          <div className="flex items-center justify-between px-5 mb-3">
            <h2 className="text-base font-semibold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Public Library</h2>
            <button className="text-xs font-medium flex items-center gap-0.5" style={{ color: "#E8A44A" }}>
              Browse <ChevronRight size={13} />
            </button>
          </div>

          {/* Genre filter */}
          <div className="flex gap-2 pl-5 overflow-x-auto pb-3" style={{ scrollbarWidth: "none" }}>
            {genres.map(g => (
              <button
                key={g}
                onClick={() => setActiveGenre(g)}
                className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap flex-shrink-0 transition-all"
                style={{
                  background: activeGenre === g ? "#E8A44A" : "#1A1A1C",
                  color: activeGenre === g ? "#0E0E0F" : "#8A8A8E",
                  fontFamily: "'Plus Jakarta Sans', sans-serif",
                }}
              >
                {g}
              </button>
            ))}
          </div>

          <div className="flex flex-col gap-3 px-5">
            {PUBLIC_BOOKS.map(book => (
              <div key={book.id} className="flex items-center gap-3 p-3 rounded-xl" style={{ background: "#1A1A1C" }}>
                <img src={book.cover} alt={book.title} className="w-14 h-20 object-cover rounded-lg flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-[10px] font-medium uppercase tracking-wider mb-0.5" style={{ color: "#E8A44A" }}>{book.genre}</p>
                  <h3 className="text-sm font-semibold leading-tight mb-0.5 truncate" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</h3>
                  <p className="text-xs mb-2" style={{ color: "#8A8A8E" }}>{book.author}</p>
                  <div className="flex items-center gap-2">
                    <Star size={11} fill="#E8A44A" style={{ color: "#E8A44A" }} />
                    <span className="text-xs font-medium" style={{ color: "#F0EBE0" }}>{book.rating}</span>
                    <span className="text-xs" style={{ color: "#8A8A8E" }}>· {book.size}</span>
                  </div>
                </div>
                <button className="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0" style={{ background: "rgba(232,164,74,0.12)", border: "1px solid rgba(232,164,74,0.25)" }}>
                  <Download size={16} style={{ color: "#E8A44A" }} />
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>

      <BottomNav active="home" onNav={onNav} />
    </div>
  );
}

function LibraryScreen({ onNav, onOpenBook }: { onNav: (s: Screen) => void; onOpenBook: (b: typeof BOOKS[0]) => void }) {
  const [tab, setTab] = useState<"all" | "reading" | "finished" | "want">("all");
  const [view, setView] = useState<"grid" | "list">("grid");
  const tabs: { id: typeof tab; label: string }[] = [
    { id: "all", label: "All" },
    { id: "reading", label: "Reading" },
    { id: "finished", label: "Finished" },
    { id: "want", label: "Want to Read" },
  ];
  const localBooks = BOOKS.filter(b => b.local);
  const filtered = tab === "all" ? BOOKS : BOOKS.filter(b => b.status === tab);

  return (
    <div className="flex flex-col h-full" style={{ background: "#0E0E0F" }}>
      {/* Header */}
      <div className="px-5 pt-4 pb-3 flex-shrink-0">
        <div className="flex items-center justify-between mb-4">
          <h1 className="text-xl font-bold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>My Library</h1>
          <div className="flex items-center gap-2">
            <button className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: "#1A1A1C" }}
              onClick={() => setView(v => v === "grid" ? "list" : "grid")}>
              {view === "grid" ? <List size={16} style={{ color: "#F0EBE0" }} /> : <Grid size={16} style={{ color: "#F0EBE0" }} />}
            </button>
            <button className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: "#1A1A1C" }}>
              <Filter size={16} style={{ color: "#F0EBE0" }} />
            </button>
          </div>
        </div>

        {/* Import banner */}
        <button className="w-full flex items-center gap-3 p-3 rounded-xl mb-4" style={{ background: "rgba(232,164,74,0.08)", border: "1px dashed rgba(232,164,74,0.3)" }}>
          <div className="w-9 h-9 rounded-lg flex items-center justify-center" style={{ background: "rgba(232,164,74,0.12)" }}>
            <FolderOpen size={18} style={{ color: "#E8A44A" }} />
          </div>
          <div className="text-left">
            <p className="text-sm font-semibold" style={{ color: "#E8A44A", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Import from device</p>
            <p className="text-xs" style={{ color: "#8A8A8E" }}>EPUB, PDF, MOBI supported</p>
          </div>
          <Upload size={16} style={{ color: "#E8A44A" }} className="ml-auto" />
        </button>

        {/* Tabs */}
        <div className="flex gap-1 p-1 rounded-xl" style={{ background: "#1A1A1C" }}>
          {tabs.map(t => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className="flex-1 py-1.5 rounded-lg text-[11px] font-medium transition-all"
              style={{
                background: tab === t.id ? "#E8A44A" : "transparent",
                color: tab === t.id ? "#0E0E0F" : "#8A8A8E",
                fontFamily: "'Plus Jakarta Sans', sans-serif",
              }}>
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {/* Book list */}
      <div className="flex-1 overflow-y-auto px-5 pb-2" style={{ scrollbarWidth: "none" }}>
        {/* Local section indicator */}
        {tab === "all" && (
          <div className="flex items-center gap-2 mb-3">
            <Smartphone size={12} style={{ color: "#8A8A8E" }} />
            <span className="text-xs" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{localBooks.length} books from device</span>
          </div>
        )}

        {view === "grid" ? (
          <div className="grid grid-cols-3 gap-3">
            {filtered.map(book => (
              <button key={book.id} onClick={() => onOpenBook(book)} className="flex flex-col gap-1.5 active:opacity-70">
                <div className="relative">
                  <img src={book.cover} alt={book.title} className="w-full aspect-[2/3] object-cover rounded-lg" />
                  {book.local && (
                    <div className="absolute top-1.5 left-1.5 px-1.5 py-0.5 rounded text-[9px] font-semibold" style={{ background: "rgba(91,143,212,0.9)", color: "#fff" }}>LOCAL</div>
                  )}
                  {book.status === "reading" && book.progress > 0 && (
                    <div className="absolute bottom-0 left-0 right-0 h-1 rounded-b-lg" style={{ background: "rgba(0,0,0,0.5)" }}>
                      <div className="h-full rounded-b-lg" style={{ width: `${book.progress}%`, background: book.highlight }} />
                    </div>
                  )}
                  {book.status === "finished" && (
                    <div className="absolute bottom-2 right-2">
                      <CheckCircle size={16} fill="#7EC8A0" style={{ color: "#7EC8A0" }} />
                    </div>
                  )}
                </div>
                <div>
                  <p className="text-[11px] font-semibold leading-tight truncate" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</p>
                  <p className="text-[10px] truncate" style={{ color: "#8A8A8E" }}>{book.author}</p>
                </div>
              </button>
            ))}
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {filtered.map(book => (
              <button key={book.id} onClick={() => onOpenBook(book)} className="flex items-center gap-3 p-3 rounded-xl active:opacity-70" style={{ background: "#1A1A1C" }}>
                <img src={book.cover} alt={book.title} className="w-12 h-16 object-cover rounded-lg flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    {book.local && <span className="text-[9px] font-semibold px-1.5 py-0.5 rounded" style={{ background: "rgba(91,143,212,0.2)", color: "#5B8FD4" }}>LOCAL</span>}
                    <p className="text-[10px]" style={{ color: "#8A8A8E" }}>{book.genre}</p>
                  </div>
                  <p className="text-sm font-semibold truncate" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</p>
                  <p className="text-xs mb-2" style={{ color: "#8A8A8E" }}>{book.author}</p>
                  {book.status === "reading" && (
                    <div className="flex items-center gap-2">
                      <div className="flex-1 h-1 rounded-full" style={{ background: "rgba(255,255,255,0.07)" }}>
                        <div className="h-full rounded-full" style={{ width: `${book.progress}%`, background: book.highlight }} />
                      </div>
                      <span className="text-[10px]" style={{ color: "#8A8A8E" }}>p. {book.currentPage}/{book.pages}</span>
                    </div>
                  )}
                  {book.status === "finished" && (
                    <div className="flex items-center gap-1">
                      <CheckCircle size={11} style={{ color: "#7EC8A0" }} />
                      <span className="text-[10px]" style={{ color: "#7EC8A0" }}>Finished</span>
                    </div>
                  )}
                  {book.status === "want" && (
                    <div className="flex items-center gap-1">
                      <Clock size={11} style={{ color: "#8A8A8E" }} />
                      <span className="text-[10px]" style={{ color: "#8A8A8E" }}>Want to read</span>
                    </div>
                  )}
                </div>
                <ChevronRight size={16} style={{ color: "#8A8A8E" }} />
              </button>
            ))}
          </div>
        )}
      </div>

      <BottomNav active="library" onNav={onNav} />
    </div>
  );
}

function ReaderScreen({ book, onBack }: { book: typeof BOOKS[0]; onBack: () => void }) {
  const [showControls, setShowControls] = useState(true);
  const [fontSize, setFontSize] = useState(17);
  const [isDark, setIsDark] = useState(true);
  const [showHighlightMenu, setShowHighlightMenu] = useState(false);
  const [showSettingsMenu, setShowSettingsMenu] = useState(false);
  const [bookmarked, setBookmarked] = useState(false);
  const [selectedHighlightColor, setSelectedHighlightColor] = useState("#E8A44A");

  const bgColor = isDark ? "#0E0E0F" : "#F5F0E8";
  const textColor = isDark ? "#D4C9B8" : "#2A2218";
  const surfaceColor = isDark ? "#1A1A1C" : "#EDE8DC";

  const HIGHLIGHT_COLORS = ["#E8A44A", "#5B8FD4", "#7EC8A0", "#D46B6B", "#B07ED4"];

  const paragraphs = READER_TEXT.trim().split("\n\n");

  return (
    <div className="flex flex-col h-full" style={{ background: bgColor, transition: "background 0.3s" }}>
      {/* Top bar */}
      {showControls && (
        <div className="flex items-center justify-between px-4 pt-4 pb-3 flex-shrink-0" style={{ borderBottom: `1px solid ${isDark ? "rgba(255,255,255,0.06)" : "rgba(0,0,0,0.06)"}` }}>
          <button onClick={onBack} className="p-1.5 -ml-1.5">
            <ArrowLeft size={20} style={{ color: textColor }} />
          </button>
          <div className="text-center">
            <p className="text-xs font-semibold" style={{ color: textColor, fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</p>
            <p className="text-[10px]" style={{ color: isDark ? "#8A8A8E" : "#9A9080" }}>Chapter 7 · {book.progress}%</p>
          </div>
          <div className="flex items-center gap-1">
            <button onClick={() => setBookmarked(b => !b)} className="p-1.5">
              <Bookmark size={18} fill={bookmarked ? "#E8A44A" : "none"} style={{ color: bookmarked ? "#E8A44A" : textColor }} />
            </button>
            <button onClick={() => setShowSettingsMenu(s => !s)} className="p-1.5">
              <MoreVertical size={18} style={{ color: textColor }} />
            </button>
          </div>
        </div>
      )}

      {/* Settings menu */}
      {showSettingsMenu && (
        <div className="mx-4 rounded-xl p-4 flex-shrink-0 mb-1" style={{ background: surfaceColor, border: `1px solid ${isDark ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.07)"}` }}>
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-semibold" style={{ color: textColor, fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Display Settings</span>
            <button onClick={() => setShowSettingsMenu(false)}>
              <X size={16} style={{ color: isDark ? "#8A8A8E" : "#9A9080" }} />
            </button>
          </div>
          {/* Font size */}
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs" style={{ color: isDark ? "#8A8A8E" : "#9A9080", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Font Size</span>
            <div className="flex items-center gap-3">
              <button onClick={() => setFontSize(f => Math.max(13, f - 1))} className="w-7 h-7 rounded-full flex items-center justify-center" style={{ background: isDark ? "#242426" : "#DDD8CC" }}>
                <Minus size={12} style={{ color: textColor }} />
              </button>
              <span className="text-sm font-medium w-6 text-center" style={{ color: textColor }}>{fontSize}</span>
              <button onClick={() => setFontSize(f => Math.min(24, f + 1))} className="w-7 h-7 rounded-full flex items-center justify-center" style={{ background: isDark ? "#242426" : "#DDD8CC" }}>
                <Plus size={12} style={{ color: textColor }} />
              </button>
            </div>
          </div>
          {/* Theme toggle */}
          <div className="flex items-center justify-between">
            <span className="text-xs" style={{ color: isDark ? "#8A8A8E" : "#9A9080", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Theme</span>
            <div className="flex items-center gap-2">
              <button onClick={() => setIsDark(false)} className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs" style={{ background: !isDark ? "#E8A44A" : isDark ? "#242426" : "#DDD8CC", color: !isDark ? "#0E0E0F" : textColor }}>
                <Sun size={12} /> Light
              </button>
              <button onClick={() => setIsDark(true)} className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs" style={{ background: isDark ? "#E8A44A" : isDark ? "#242426" : "#DDD8CC", color: isDark ? "#0E0E0F" : textColor }}>
                <Moon size={12} /> Dark
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Highlight color picker */}
      {showHighlightMenu && (
        <div className="mx-4 rounded-xl p-4 flex-shrink-0 mb-1" style={{ background: surfaceColor, border: `1px solid ${isDark ? "rgba(255,255,255,0.07)" : "rgba(0,0,0,0.07)"}` }}>
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-semibold" style={{ color: textColor, fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Highlight</span>
            <button onClick={() => setShowHighlightMenu(false)}>
              <X size={16} style={{ color: isDark ? "#8A8A8E" : "#9A9080" }} />
            </button>
          </div>
          <div className="flex items-center gap-2 mb-3">
            {HIGHLIGHT_COLORS.map(c => (
              <button key={c} onClick={() => setSelectedHighlightColor(c)} className="w-8 h-8 rounded-full transition-transform" style={{ background: c, transform: selectedHighlightColor === c ? "scale(1.2)" : "scale(1)", outline: selectedHighlightColor === c ? `2px solid ${c}` : "none", outlineOffset: "2px" }} />
            ))}
          </div>
          <div className="flex gap-2">
            {[
              { icon: Highlighter, label: "Highlight" },
              { icon: StickyNote, label: "Add Note" },
              { icon: Share2, label: "Share" },
            ].map(({ icon: Icon, label }) => (
              <button key={label} className="flex-1 flex flex-col items-center gap-1 py-2 rounded-lg text-[10px]"
                style={{ background: isDark ? "#242426" : "#DDD8CC", color: textColor, fontFamily: "'Plus Jakarta Sans', sans-serif" }}>
                <Icon size={14} />
                {label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Reading content */}
      <div
        className="flex-1 overflow-y-auto px-6 py-4"
        style={{ scrollbarWidth: "none" }}
        onClick={() => { setShowControls(s => !s); setShowSettingsMenu(false); }}
      >
        {paragraphs.map((para, i) => (
          <p key={i}
            className={`leading-relaxed mb-5 ${i === 0 ? "font-semibold text-base" : ""}`}
            style={{
              fontFamily: "'Literata', Georgia, serif",
              fontSize: i === 0 ? `${fontSize + 1}px` : `${fontSize}px`,
              color: i === 0 ? (isDark ? "#F0EBE0" : "#1A1510") : textColor,
            }}
          >
            {i === 2 ? (
              <>
                {para.slice(0, 70)}
                <span
                  onClick={e => { e.stopPropagation(); setShowHighlightMenu(true); }}
                  style={{ background: `${selectedHighlightColor}35`, borderBottom: `2px solid ${selectedHighlightColor}`, cursor: "pointer" }}
                >
                  {para.slice(70, 155)}
                </span>
                {para.slice(155)}
              </>
            ) : para}
          </p>
        ))}
        <div className="h-20" />
      </div>

      {/* Bottom bar */}
      {showControls && (
        <div className="px-5 pb-4 pt-3 flex-shrink-0" style={{ borderTop: `1px solid ${isDark ? "rgba(255,255,255,0.06)" : "rgba(0,0,0,0.06)"}` }}>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs" style={{ color: isDark ? "#8A8A8E" : "#9A9080", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>p. {book.currentPage}</span>
            <span className="text-xs font-medium" style={{ color: textColor, fontFamily: "'Plus Jakarta Sans', sans-serif" }}>The 2-Minute Rule</span>
            <span className="text-xs" style={{ color: isDark ? "#8A8A8E" : "#9A9080", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.pages - book.currentPage} left</span>
          </div>
          <div className="w-full h-1 rounded-full mb-4" style={{ background: isDark ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)" }}>
            <div className="h-full rounded-full" style={{ width: `${book.progress}%`, background: "#E8A44A" }} />
          </div>
          <div className="flex items-center justify-around">
            {[
              { icon: Highlighter, label: "Highlight", action: () => setShowHighlightMenu(s => !s) },
              { icon: StickyNote, label: "Note", action: () => {} },
              { icon: BookOpen, label: "Contents", action: () => {} },
              { icon: Share2, label: "Share", action: () => {} },
            ].map(({ icon: Icon, label, action }) => (
              <button key={label} onClick={e => { e.stopPropagation(); action(); }} className="flex flex-col items-center gap-1">
                <Icon size={18} style={{ color: isDark ? "#8A8A8E" : "#9A9080" }} />
                <span className="text-[9px]" style={{ color: isDark ? "#8A8A8E" : "#9A9080", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{label}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function NotesScreen({ onNav }: { onNav: (s: Screen) => void }) {
  const [activeBook, setActiveBook] = useState<number | null>(null);
  const bookIds = [...new Set(HIGHLIGHTS.map(h => h.bookId))];
  const filtered = activeBook ? HIGHLIGHTS.filter(h => h.bookId === activeBook) : HIGHLIGHTS;

  return (
    <div className="flex flex-col h-full" style={{ background: "#0E0E0F" }}>
      <div className="px-5 pt-4 pb-3 flex-shrink-0">
        <h1 className="text-xl font-bold mb-1" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Notes & Highlights</h1>
        <p className="text-xs mb-4" style={{ color: "#8A8A8E" }}>{HIGHLIGHTS.length} highlights across {bookIds.length} books</p>

        {/* Book filter */}
        <div className="flex gap-2 overflow-x-auto pb-1" style={{ scrollbarWidth: "none" }}>
          <button
            onClick={() => setActiveBook(null)}
            className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap flex-shrink-0"
            style={{ background: !activeBook ? "#E8A44A" : "#1A1A1C", color: !activeBook ? "#0E0E0F" : "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          >
            All books
          </button>
          {bookIds.map(bid => {
            const book = BOOKS.find(b => b.id === bid);
            return book ? (
              <button key={bid}
                onClick={() => setActiveBook(bid === activeBook ? null : bid)}
                className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap flex-shrink-0 flex items-center gap-1.5"
                style={{ background: activeBook === bid ? "#E8A44A" : "#1A1A1C", color: activeBook === bid ? "#0E0E0F" : "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
              >
                {book.title}
              </button>
            ) : null;
          })}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-5 pb-2" style={{ scrollbarWidth: "none" }}>
        {bookIds.filter(bid => !activeBook || bid === activeBook).map(bid => {
          const book = BOOKS.find(b => b.id === bid);
          const bookHighlights = filtered.filter(h => h.bookId === bid);
          if (!book || bookHighlights.length === 0) return null;
          return (
            <div key={bid} className="mb-6">
              {/* Book header */}
              <div className="flex items-center gap-2 mb-3">
                <img src={book.cover} alt={book.title} className="w-8 h-11 object-cover rounded-md" />
                <div>
                  <p className="text-sm font-semibold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</p>
                  <p className="text-xs" style={{ color: "#8A8A8E" }}>{bookHighlights.length} highlights</p>
                </div>
              </div>

              <div className="flex flex-col gap-2 pl-1">
                {bookHighlights.map(h => (
                  <div key={h.id} className="rounded-xl overflow-hidden" style={{ background: "#1A1A1C" }}>
                    <div className="h-1 w-full" style={{ background: h.color }} />
                    <div className="p-3">
                      <blockquote className="leading-relaxed mb-2" style={{ fontFamily: "'Literata', Georgia, serif", fontSize: "13px", color: "#D4C9B8", borderLeft: `2px solid ${h.color}`, paddingLeft: "10px" }}>
                        "{h.text}"
                      </blockquote>
                      {h.note && (
                        <div className="flex items-start gap-1.5 mb-2 mt-2 pl-3">
                          <PenLine size={11} style={{ color: "#8A8A8E", marginTop: "2px" }} />
                          <p className="text-[11px] leading-relaxed" style={{ color: "#8A8A8E", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{h.note}</p>
                        </div>
                      )}
                      <div className="flex items-center justify-between pl-3">
                        <div className="flex items-center gap-2">
                          <span className="text-[10px]" style={{ color: "#8A8A8E" }}>p. {h.page}</span>
                          <span className="text-[10px]" style={{ color: "#8A8A8E" }}>· {h.date}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <button className="p-1">
                            <Share2 size={13} style={{ color: "#8A8A8E" }} />
                          </button>
                          <button className="p-1">
                            <Trash2 size={13} style={{ color: "#8A8A8E" }} />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      <BottomNav active="notes" onNav={onNav} />
    </div>
  );
}

function SearchScreen({ onNav, onOpenBook }: { onNav: (s: Screen) => void; onOpenBook: (b: typeof BOOKS[0]) => void }) {
  const [query, setQuery] = useState("");
  const results = query.length > 1 ? BOOKS.filter(b =>
    b.title.toLowerCase().includes(query.toLowerCase()) ||
    b.author.toLowerCase().includes(query.toLowerCase())
  ) : [];

  return (
    <div className="flex flex-col h-full" style={{ background: "#0E0E0F" }}>
      <div className="px-5 pt-4 pb-3 flex-shrink-0">
        <h1 className="text-xl font-bold mb-3" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Discover</h1>
        <div className="flex items-center gap-3 px-4 py-3 rounded-xl" style={{ background: "#1A1A1C" }}>
          <Search size={16} style={{ color: "#8A8A8E" }} />
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Search books, authors..."
            className="flex-1 bg-transparent outline-none text-sm"
            style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          />
          {query && <button onClick={() => setQuery("")}><X size={14} style={{ color: "#8A8A8E" }} /></button>}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-5 pb-2" style={{ scrollbarWidth: "none" }}>
        {!query && (
          <>
            <h2 className="text-sm font-semibold mb-3" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Popular Genres</h2>
            <div className="grid grid-cols-2 gap-2 mb-6">
              {[
                { name: "Fiction", color: "#5B8FD4", img: "photo-1481627834876-b7833e8f5570" },
                { name: "Self-Help", color: "#E8A44A", img: "photo-1544716278-ca5e3f4abd8c" },
                { name: "History", color: "#7EC8A0", img: "photo-1524995997946-a1c2e315a42f" },
                { name: "Science", color: "#B07ED4", img: "photo-1507842217343-583bb7270b66" },
              ].map(g => (
                <div key={g.name} className="relative h-20 rounded-xl overflow-hidden" style={{ background: "#1A1A1C" }}>
                  <img src={`https://images.unsplash.com/${g.img}?w=300&h=160&fit=crop&auto=format`} alt={g.name} className="absolute inset-0 w-full h-full object-cover opacity-30" />
                  <div className="absolute inset-0 p-3 flex items-end">
                    <span className="text-sm font-bold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{g.name}</span>
                  </div>
                  <div className="absolute top-2 right-2 w-2 h-2 rounded-full" style={{ background: g.color }} />
                </div>
              ))}
            </div>
            <h2 className="text-sm font-semibold mb-3" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Trending Now</h2>
            <div className="flex flex-col gap-2">
              {PUBLIC_BOOKS.map(book => (
                <div key={book.id} className="flex items-center gap-3 p-3 rounded-xl" style={{ background: "#1A1A1C" }}>
                  <img src={book.cover} alt={book.title} className="w-11 h-16 object-cover rounded-lg" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold truncate" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</p>
                    <p className="text-xs" style={{ color: "#8A8A8E" }}>{book.author}</p>
                    <div className="flex items-center gap-1 mt-1">
                      <Star size={10} fill="#E8A44A" style={{ color: "#E8A44A" }} />
                      <span className="text-[10px]" style={{ color: "#F0EBE0" }}>{book.rating}</span>
                    </div>
                  </div>
                  <button className="w-8 h-8 rounded-full flex items-center justify-center" style={{ background: "rgba(232,164,74,0.1)" }}>
                    <Download size={14} style={{ color: "#E8A44A" }} />
                  </button>
                </div>
              ))}
            </div>
          </>
        )}

        {query.length > 1 && results.length > 0 && (
          <div className="flex flex-col gap-2">
            {results.map(book => (
              <button key={book.id} onClick={() => onOpenBook(book)} className="flex items-center gap-3 p-3 rounded-xl" style={{ background: "#1A1A1C" }}>
                <img src={book.cover} alt={book.title} className="w-11 h-16 object-cover rounded-lg" />
                <div className="flex-1 text-left">
                  <p className="text-sm font-semibold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{book.title}</p>
                  <p className="text-xs" style={{ color: "#8A8A8E" }}>{book.author}</p>
                </div>
              </button>
            ))}
          </div>
        )}

        {query.length > 1 && results.length === 0 && (
          <div className="flex flex-col items-center py-12 gap-3">
            <Search size={36} style={{ color: "#3A3A3E" }} />
            <p className="text-sm" style={{ color: "#8A8A8E" }}>No results for "{query}"</p>
          </div>
        )}
      </div>

      <BottomNav active="search" onNav={onNav} />
    </div>
  );
}

function ProfileScreen({ onNav }: { onNav: (s: Screen) => void }) {
  return (
    <div className="flex flex-col h-full" style={{ background: "#0E0E0F" }}>
      <div className="flex-1 overflow-y-auto" style={{ scrollbarWidth: "none" }}>
        {/* Header */}
        <div className="px-5 pt-6 pb-4 flex flex-col items-center gap-3 border-b" style={{ borderColor: "rgba(255,255,255,0.07)" }}>
          <div className="relative">
            <img src="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&h=120&fit=crop&auto=format" alt="avatar" className="w-20 h-20 rounded-full object-cover" style={{ border: "3px solid #E8A44A" }} />
            <button className="absolute bottom-0 right-0 w-7 h-7 rounded-full flex items-center justify-center" style={{ background: "#E8A44A" }}>
              <Edit3 size={12} style={{ color: "#0E0E0F" }} />
            </button>
          </div>
          <div className="text-center">
            <h2 className="text-lg font-bold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Minh Tuấn</h2>
            <p className="text-xs" style={{ color: "#8A8A8E" }}>minhtuannguyen@gmail.com</p>
          </div>
          <div className="flex gap-6 mt-1">
            {[{ label: "Books", value: "12" }, { label: "Pages Read", value: "3,241" }, { label: "Highlights", value: "47" }].map(s => (
              <div key={s.label} className="text-center">
                <p className="text-base font-bold" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{s.value}</p>
                <p className="text-[10px]" style={{ color: "#8A8A8E" }}>{s.label}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Stats */}
        <div className="px-5 py-4">
          <h3 className="text-sm font-semibold mb-3" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Reading Streak</h3>
          <div className="p-4 rounded-xl flex items-center gap-4" style={{ background: "rgba(232,164,74,0.08)", border: "1px solid rgba(232,164,74,0.15)" }}>
            <div className="text-center">
              <p className="text-3xl font-bold" style={{ color: "#E8A44A", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>14</p>
              <p className="text-[10px]" style={{ color: "#8A8A8E" }}>day streak</p>
            </div>
            <div className="flex-1">
              <div className="flex gap-1">
                {Array.from({ length: 14 }).map((_, i) => (
                  <div key={i} className="flex-1 rounded-sm" style={{ height: `${16 + Math.random() * 20}px`, background: i >= 10 ? "#E8A44A" : "rgba(232,164,74,0.25)" }} />
                ))}
              </div>
              <p className="text-[10px] mt-1" style={{ color: "#8A8A8E" }}>Last 14 days</p>
            </div>
          </div>
        </div>

        {/* Settings */}
        <div className="px-5 pb-4">
          <h3 className="text-sm font-semibold mb-3" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>Settings</h3>
          <div className="rounded-xl overflow-hidden" style={{ background: "#1A1A1C" }}>
            {[
              { icon: Bell, label: "Notifications" },
              { icon: Layers, label: "Reading Goals" },
              { icon: FileText, label: "Export Notes" },
              { icon: Smartphone, label: "Device Sync" },
              { icon: Settings, label: "Preferences" },
            ].map(({ icon: Icon, label }, i, arr) => (
              <button key={label} className="w-full flex items-center gap-3 px-4 py-3.5 active:opacity-70"
                style={{ borderBottom: i < arr.length - 1 ? "1px solid rgba(255,255,255,0.05)" : "none" }}>
                <Icon size={17} style={{ color: "#8A8A8E" }} />
                <span className="flex-1 text-sm text-left" style={{ color: "#F0EBE0", fontFamily: "'Plus Jakarta Sans', sans-serif" }}>{label}</span>
                <ChevronRight size={15} style={{ color: "#8A8A8E" }} />
              </button>
            ))}
          </div>
          <button
            onClick={() => onNav("login")}
            className="w-full mt-3 flex items-center justify-center gap-2 py-3 rounded-xl text-sm font-medium"
            style={{ background: "rgba(224,92,92,0.08)", color: "#E05C5C", border: "1px solid rgba(224,92,92,0.15)", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          >
            <LogOut size={16} /> Sign Out
          </button>
        </div>
      </div>

      <BottomNav active="profile" onNav={onNav} />
    </div>
  );
}

// ————— Main App —————

export default function App() {
  const [screen, setScreen] = useState<Screen>("login");
  const [activeBook, setActiveBook] = useState<typeof BOOKS[0] | null>(null);

  const handleOpenBook = (book: typeof BOOKS[0]) => {
    setActiveBook(book);
    setScreen("reader");
  };

  const handleBack = () => {
    setScreen("home");
    setActiveBook(null);
  };

  const renderScreen = () => {
    if (screen === "reader" && activeBook) return <ReaderScreen book={activeBook} onBack={handleBack} />;
    switch (screen) {
      case "login": return <LoginScreen onNav={setScreen} />;
      case "register": return <RegisterScreen onNav={setScreen} />;
      case "home": return <HomeScreen onNav={setScreen} onOpenBook={handleOpenBook} />;
      case "library": return <LibraryScreen onNav={setScreen} onOpenBook={handleOpenBook} />;
      case "notes": return <NotesScreen onNav={setScreen} />;
      case "search": return <SearchScreen onNav={setScreen} onOpenBook={handleOpenBook} />;
      case "profile": return <ProfileScreen onNav={setScreen} />;
      default: return <HomeScreen onNav={setScreen} onOpenBook={handleOpenBook} />;
    }
  };

  return (
    <div
      className="min-h-screen flex items-center justify-center py-8"
      style={{ background: "#060607", fontFamily: "'Plus Jakarta Sans', sans-serif" }}
    >
      {/* Ambient glow */}
      <div className="fixed inset-0 pointer-events-none" style={{ background: "radial-gradient(ellipse 60% 40% at 50% 0%, rgba(232,164,74,0.06) 0%, transparent 70%)" }} />

      {/* Phone frame */}
      <div
        className="relative flex flex-col overflow-hidden"
        style={{
          width: "390px",
          height: "844px",
          borderRadius: "48px",
          background: "#0E0E0F",
          boxShadow: "0 0 0 1px rgba(255,255,255,0.08), 0 40px 80px rgba(0,0,0,0.8), 0 0 0 14px #111113",
        }}
      >
        {/* Status bar */}
        <div className="flex items-center justify-between px-8 pt-3 pb-1 flex-shrink-0" style={{ height: "44px" }}>
          <span className="text-[12px] font-semibold" style={{ color: "#F0EBE0" }}>9:41</span>
          <div className="w-28 h-7 rounded-full flex-shrink-0" style={{ background: "#111" }} />
          <div className="flex items-center gap-1">
            <div className="flex gap-0.5 items-end h-3">
              {[3, 5, 7, 9].map((h, i) => <div key={i} className="w-1 rounded-sm" style={{ height: `${h}px`, background: i < 3 ? "#F0EBE0" : "rgba(240,235,224,0.3)" }} />)}
            </div>
            <svg width="16" height="12" viewBox="0 0 24 18" fill="none">
              <path d="M12 3C8.5 3 5.4 4.4 3.2 6.8L1 4.4C3.8 1.6 7.7 0 12 0s8.2 1.6 11 4.4L20.8 6.8C18.6 4.4 15.5 3 12 3z" fill="#F0EBE0" fillOpacity="0.4"/>
              <path d="M12 9c-2.2 0-4.1.9-5.5 2.4L4.3 9.2C6.2 7.2 9 6 12 6s5.8 1.2 7.7 3.2l-2.2 2.2C16.1 9.9 14.2 9 12 9z" fill="#F0EBE0" fillOpacity="0.7"/>
              <circle cx="12" cy="15" r="3" fill="#F0EBE0"/>
            </svg>
            <div className="flex items-center gap-0.5">
              <div className="w-6 h-3 rounded-sm border" style={{ borderColor: "rgba(240,235,224,0.5)" }}>
                <div className="h-full rounded-sm" style={{ width: "70%", background: "#F0EBE0" }} />
              </div>
            </div>
          </div>
        </div>

        {/* Screen content */}
        <div className="flex-1 overflow-hidden">
          {renderScreen()}
        </div>

        {/* Home indicator */}
        <div className="flex justify-center pb-2 flex-shrink-0">
          <div className="w-32 h-1 rounded-full" style={{ background: "rgba(255,255,255,0.2)" }} />
        </div>
      </div>

      {/* Screen labels below phone */}
      <div className="fixed bottom-4 left-1/2 -translate-x-1/2 flex gap-2 flex-wrap justify-center">
        {(["login", "register", "home", "library", "notes", "search", "profile"] as Screen[]).map(s => (
          <button
            key={s}
            onClick={() => { setScreen(s); setActiveBook(null); }}
            className="px-3 py-1 rounded-full text-[11px] font-medium transition-all"
            style={{
              background: screen === s ? "#E8A44A" : "rgba(255,255,255,0.07)",
              color: screen === s ? "#0E0E0F" : "rgba(255,255,255,0.5)",
              fontFamily: "'Plus Jakarta Sans', sans-serif",
            }}
          >
            {s}
          </button>
        ))}
      </div>
    </div>
  );
}
