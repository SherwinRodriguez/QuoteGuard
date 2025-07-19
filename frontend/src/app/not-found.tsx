export default function NotFound() {
  return (
    <div className="h-screen flex items-center justify-center bg-gray-100">
      <div className="text-center space-y-3">
        <h1 className="text-4xl font-bold text-red-500">404 - Page Not Found</h1>
        <p className="text-gray-600">Oops! The page you're looking for doesn't exist.</p>
        <a
          href="/"
          className="inline-block mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
        >
          Go Home
        </a>
      </div>
    </div>
  );
}