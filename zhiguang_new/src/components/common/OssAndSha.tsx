// 直传 OSS
async function uploadToPresigned(putUrl: string, headers: Record<string, string>, file: File) {
  const resp = await fetch(putUrl, { method: "PUT", headers, body: file, credentials: "omit" });
  if (!resp.ok) {
    const text = await resp.text().catch(() => "");
    throw new Error(text || `上传失败：${resp.status}`);
  }
  const etag = resp.headers.get("ETag") || resp.headers.get("etag") || "";
  return { etag };
}

// 计算 SHA-256
async function computeSha256(file: File) {
  const buf = await file.arrayBuffer();
  const digest = await crypto.subtle.digest("SHA-256", buf);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export { uploadToPresigned, computeSha256 };
