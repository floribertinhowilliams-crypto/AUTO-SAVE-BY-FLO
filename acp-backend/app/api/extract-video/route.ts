import { NextRequest, NextResponse } from "next/server";

export const maxDuration = 30;

interface ExtractResult {
  success: boolean;
  videoUrl?: string;
  referer?: string;
  platform?: "tiktok" | "instagram" | "unknown";
  error?: string;
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const url: string | undefined = body?.url;

    if (!url || typeof url !== "string") {
      return NextResponse.json(
        { success: false, error: "Missing 'url' in request body" } as ExtractResult,
        { status: 400 }
      );
    }

    const cleanUrl = url.trim();

    let result: ExtractResult;

    if (cleanUrl.includes("tiktok.com")) {
      result = await extractTikTok(cleanUrl);
    } else if (cleanUrl.includes("instagram.com")) {
      result = await extractInstagram(cleanUrl);
    } else if (cleanUrl.match(/\.(mp4|mov|webm)(\?.*)?$/i)) {
      result = { success: true, videoUrl: cleanUrl, platform: "unknown" };
    } else {
      result = { success: false, error: "Unsupported URL - siyo TikTok, Instagram, au direct video link" };
    }

    return NextResponse.json(result, { status: result.success ? 200 : 422 });
  } catch (err: any) {
    return NextResponse.json(
      { success: false, error: err?.message || "Server error wakati wa extraction" } as ExtractResult,
      { status: 500 }
    );
  }
}

async function extractTikTok(url: string): Promise<ExtractResult> {
  try {
    const TiktokMod: any = await import("@tobyg74/tiktok-api-dl");
    const Tiktok = TiktokMod.default ?? TiktokMod;

    const response = await Tiktok.Downloader(url, { version: "v1" });

    if (response?.status !== "success" || !response?.result) {
      return { success: false, error: "TikTok haikurudisha video data (huenda link si sahihi au video ni ya siri)" };
    }

    const videoUrl: string | undefined =
      response.result?.video?.playAddr?.[0] ||
      response.result?.video?.downloadAddr?.[0] ||
      response.result?.video1 ||
      response.result?.video;

    if (!videoUrl) {
      return { success: false, error: "Video URL haikupatikana kwenye response ya TikTok" };
    }

    return {
      success: true,
      videoUrl,
      referer: "https://www.tiktok.com/",
      platform: "tiktok",
    };
  } catch (err: any) {
    return { success: false, error: `TikTok extraction error: ${err?.message || err}` };
  }
}

async function extractInstagram(url: string): Promise<ExtractResult> {
  try {
    const igMod: any = await import("instagram-url-direct");
    const instagramGetUrl = igMod.instagramGetUrl ?? igMod.default?.instagramGetUrl;

    const data = await instagramGetUrl(url);
    const videoUrl: string | undefined = data?.url_list?.[0];

    if (!videoUrl) {
      return { success: false, error: "Video URL haikupatikana - reel hii huenda ni private au format haitambuliki" };
    }

    return {
      success: true,
      videoUrl,
      referer: "https://www.instagram.com/",
      platform: "instagram",
    };
  } catch (err: any) {
    return { success: false, error: `Instagram extraction error: ${err?.message || err}` };
  }
}
