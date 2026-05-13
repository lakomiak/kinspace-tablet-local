from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from matplotlib.backends.backend_pdf import PdfPages
import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parent
OUT_MD = ROOT / "USER_GUIDE.md"
OUT_PDF = ROOT / "USER_GUIDE.pdf"
TEMP_PDF = ROOT / "USER_GUIDE.tmp.pdf"

PAGE_W = 2550
PAGE_H = 3300
MARGIN = 120
CONTENT_W = PAGE_W - (MARGIN * 2)
CARD_RADIUS = 42

BG = "#F8F4EC"
PURPLE = "#DCCFF4"
PURPLE_DARK = "#2E1A63"
GREEN = "#6E9E58"
TEXT = "#263238"
MUTED = "#5B5B5B"
CARD = "#FFFFFF"
CARD_SOFT = "#F1EAF8"
LINE = "#DDD7E6"

FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

if not Path(FONT_REG).exists():
    FONT_REG = r"C:\Windows\Fonts\segoeui.ttf"
if not Path(FONT_BOLD).exists():
    FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"


@dataclass
class GuidePage:
    kind: str
    title: str = ""
    subtitle: str = ""
    bullets: list[str] | None = None
    screenshot: Path | None = None
    caption: str | None = None


PAGES = [
    GuidePage(
        kind="cover",
        title="Kinspace Tablet User Guide",
        subtitle="Simple instructions for using the family tablet at home.",
        bullets=[
            "Start with Kinspace sign in",
            "Choose the family member for this tablet",
            "See today's tasks",
            "Move to another day",
            "Complete or uncomplete tasks",
            "Add a new to-do",
            "Start and finish a timer",
            "Review or change settings",
        ],
    ),
    GuidePage(
        kind="section",
        title="Onboarding and Kinspace Cloud Login",
        subtitle="The first launch uses Kinspace Cloud to sign in, then chooses who the tablet belongs to.",
        bullets=[
            "Tap Sign in with Kinspace on the first screen.",
            "Complete the Kinspace Cloud login in the browser flow.",
            "If the tablet has not been assigned yet, choose the family member who uses this tablet.",
            "That selection saves the tablet's member and household so the Home screen can load the right tasks.",
            "If you ever need to change who the tablet belongs to, Settings can reset the assignment and send you back through setup.",
        ],
        screenshot=None,
        caption=None,
    ),
    GuidePage(
        kind="section",
        title="Home Screen",
        subtitle="This is the main screen families use every day.",
        bullets=[
            "The top bar greets the active family member.",
            "Use the date controls to move between days.",
            "Tap Pick date to jump directly to any date.",
            "Tap Today to return to the current day.",
            "Tap the red circle to mark a to-do complete or uncomplete.",
            "Tasks with timers show a Start button.",
            "If editing is enabled in Settings, Edit and Delete buttons appear on the task card.",
            "Past days are completion-only, so you can review history without changing the task itself.",
        ],
        screenshot=ROOT / "tablet-guide-home.png",
        caption="Home shows today's tasks, progress, and day navigation.",
    ),
    GuidePage(
        kind="section",
        title="Add To Do",
        subtitle="Use the Add To Do screen to create a new task for a family member.",
        bullets=[
            "Tap the purple + button on the Home screen to start a new to-do.",
            "Enter a title first.",
            "Tap the due date row to open the calendar picker.",
            "Leave the due date blank if the task should count for today.",
            "Choose the todo group and repeat options from the native pickers.",
            "Timer minutes and seconds use native number pickers.",
            "Minutes and seconds can both be 0.",
            "Tap Save To Do to save the item and sync it to the cloud.",
        ],
        screenshot=ROOT / "tablet-guide-add.png",
        caption="The add form uses native pickers for date, group, repeat, and timer values.",
    ),
    GuidePage(
        kind="section",
        title="Timer Screen",
        subtitle="Start a timer when a task has a saved time allowance.",
        bullets=[
            "Tap Start on a task that has a timer.",
            "Use Pause / Resume to stop and restart the countdown.",
            "Use Complete To Do to mark the task complete from the timer view.",
            "Use Reset Timer to restart the timer from the beginning.",
            "Use Cancel to leave the timer without completing the task.",
            "When the timer ends, the tablet plays the selected alarm sound and stays on the timer screen until the user leaves it.",
        ],
        screenshot=ROOT / "tablet-guide-timer.png",
        caption="The timer screen keeps the user in control until they finish or cancel.",
    ),
    GuidePage(
        kind="section",
        title="Settings Screen",
        subtitle="Settings are protected so changes can be controlled.",
        bullets=[
            "Settings can be locked with a 5-digit passcode.",
            "If Settings are locked, enter the passcode to unlock them.",
            "Use Reset via Cloud if the passcode is forgotten.",
            "The Behavior section controls the daily reset time and auto-logout timeout.",
            "The edit/delete todo toggle controls whether task cards can be changed or removed.",
            "After leaving Settings, the passcode lock is restored the next time the screen opens.",
        ],
        screenshot=ROOT / "tablet-guide-settings.png",
        caption="Settings includes the passcode lock and behavior controls.",
    ),
    GuidePage(
        kind="section",
        title="Editing and Deleting To Dos",
        subtitle="Turn on the setting first, then use the buttons on each card.",
        bullets=[
            "Open Settings and unlock them first if needed.",
            "Turn on the Allow edit/delete To Dos setting.",
            "Go back to Home to see the Edit and Delete buttons on each task card.",
            "Edit changes the task details and saves the update to the cloud.",
            "Delete removes the task entirely and syncs the removal to the cloud.",
            "If the setting is off, the buttons stay hidden.",
        ],
        screenshot=ROOT / "tablet-guide-home.png",
        caption="Edit and Delete appear on Home when the setting is enabled.",
    ),
    GuidePage(
        kind="section",
        title="Passcode Recovery",
        subtitle="If the passcode is forgotten, the tablet can be reset through cloud login.",
        bullets=[
            "Go to Settings.",
            "Tap Reset via Cloud.",
            "Sign in to Kinspace Cloud.",
            "After successful cloud login, set a new 5-digit passcode on the tablet.",
            "This keeps the tablet protected without losing access permanently.",
        ],
        screenshot=ROOT / "tablet-guide-settings.png",
        caption="Reset via Cloud is the recovery path when the local passcode is forgotten.",
    ),
    GuidePage(
        kind="troubleshoot",
        title="Troubleshooting",
        subtitle="A few quick fixes for common issues.",
        bullets=[
            "If a new to-do does not appear, tap the refresh icon on Home.",
            "If the cloud and tablet disagree, refresh Home again after a moment.",
            "If Settings are locked, use the passcode or Reset via Cloud.",
            "If a task has no Start button, it does not have a saved timer.",
            "If you are looking at a previous day, only completion changes are allowed.",
        ],
    ),
]


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    path = FONT_BOLD if bold else FONT_REG
    return ImageFont.truetype(path, size=size)


def wrap_text(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = f"{current} {word}".strip()
        bbox = draw.textbbox((0, 0), candidate, font=font)
        width = bbox[2] - bbox[0]
        if width <= max_width or not current:
            current = candidate
        else:
            lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_wrapped(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    text: str,
    font: ImageFont.FreeTypeFont,
    fill: str,
    max_width: int,
    line_gap: int = 10,
) -> int:
    lines = wrap_text(draw, text, font, max_width)
    bbox = draw.textbbox((0, 0), "Ag", font=font)
    line_height = bbox[3] - bbox[1]
    for line in lines:
        draw.text((x, y), line, fill=fill, font=font)
        y += line_height + line_gap
    return y


def rounded_box(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    fill: str,
    outline: str | None = None,
    width: int = 2,
    radius: int = CARD_RADIUS,
):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width if outline else 0)


def fit_image(image_path: Path, max_w: int, max_h: int) -> Image.Image:
    img = Image.open(image_path).convert("RGB")
    img.thumbnail((max_w, max_h), Image.Resampling.LANCZOS)
    return img


def add_page_number(draw: ImageDraw.ImageDraw, page_num: int, total: int):
    font = load_font(28, bold=False)
    label = f"{page_num} / {total}"
    bbox = draw.textbbox((0, 0), label, font=font)
    w = bbox[2] - bbox[0]
    draw.text((PAGE_W - MARGIN - w, PAGE_H - 72), label, fill=MUTED, font=font)


def build_cover(page: GuidePage, page_num: int, total: int) -> Image.Image:
    img = Image.new("RGB", (PAGE_W, PAGE_H), BG)
    draw = ImageDraw.Draw(img)
    rounded_box(draw, (0, 0, PAGE_W, PAGE_H), fill=BG)

    title_font = load_font(96, bold=True)
    subtitle_font = load_font(48, bold=False)
    bullet_font = load_font(40, bold=False)
    label_font = load_font(34, bold=True)

    rounded_box(draw, (0, 0, PAGE_W, 420), fill=PURPLE)
    draw.text((MARGIN, 80), page.title, fill=PURPLE_DARK, font=title_font)
    draw.text((MARGIN, 190), page.subtitle, fill=PURPLE_DARK, font=subtitle_font)

    # Feature card
    rounded_box(draw, (MARGIN, 520, PAGE_W - MARGIN, 1590), fill=CARD, outline=LINE, width=3)
    draw.text((MARGIN + 60, 580), "What this guide covers", fill=GREEN, font=label_font)
    y = 680
    max_width = CONTENT_W - 140
    for bullet in page.bullets or []:
        draw.text((MARGIN + 60, y), "•", fill=GREEN, font=bullet_font)
        y = draw_wrapped(draw, MARGIN + 110, y, bullet, bullet_font, TEXT, max_width - 50, line_gap=12)
        y += 26

    # Simple callout card
    rounded_box(draw, (MARGIN, 1700, PAGE_W - MARGIN, 2360), fill=CARD_SOFT, outline=LINE, width=3)
    draw.text((MARGIN + 60, 1760), "How to use this guide", fill=PURPLE_DARK, font=label_font)
    body_font = load_font(38, bold=False)
    y = 1860
    tips = [
        "Start on Home to see the current day.",
        "Use Add To Do to create tasks with due dates and timers.",
        "Use the Timer screen when a task has a saved duration.",
        "Use Settings to protect changes and manage editing.",
        "Use Troubleshooting if the tablet and cloud get out of sync.",
    ]
    for tip in tips:
        draw.text((MARGIN + 60, y), "•", fill=PURPLE_DARK, font=body_font)
        y = draw_wrapped(draw, MARGIN + 110, y, tip, body_font, TEXT, max_width - 50, line_gap=12)
        y += 20

    add_page_number(draw, page_num, total)
    return img


def build_content(page: GuidePage, page_num: int, total: int) -> Image.Image:
    img = Image.new("RGB", (PAGE_W, PAGE_H), BG)
    draw = ImageDraw.Draw(img)

    rounded_box(draw, (0, 0, PAGE_W, 260), fill=PURPLE)
    title_font = load_font(84, bold=True)
    subtitle_font = load_font(42, bold=False)
    body_font = load_font(35, bold=False)
    small_bold = load_font(32, bold=True)

    draw.text((MARGIN, 72), "Kinspace Tablet User Guide", fill=PURPLE_DARK, font=title_font)
    draw.text((MARGIN, 182), "A quick guide for using the tablet at home", fill=PURPLE_DARK, font=subtitle_font)

    y = 330
    draw.text((MARGIN, y), page.title, fill=TEXT, font=load_font(60, bold=True))
    y += 88
    draw.text((MARGIN, y), page.subtitle, fill=MUTED, font=subtitle_font)
    y += 92

    if page.kind == "troubleshoot":
        bullets_top = y
        bullets_bottom = PAGE_H - 240
        rounded_box(draw, (MARGIN, bullets_top, PAGE_W - MARGIN, bullets_bottom), fill=CARD, outline=LINE, width=3)
        inner_x = MARGIN + 50
        cursor_y = bullets_top + 40
        draw.text((inner_x, cursor_y), "What to try first", fill=GREEN, font=small_bold)
        cursor_y += 58
        bullet_max_width = CONTENT_W - 120
        for bullet in page.bullets or []:
            draw.text((inner_x, cursor_y), "•", fill=GREEN, font=body_font)
            cursor_y = draw_wrapped(draw, inner_x + 38, cursor_y, bullet, body_font, TEXT, bullet_max_width - 38, line_gap=12)
            cursor_y += 20
    else:
        bullets_top = y
        bullets_bottom = bullets_top + 700
        rounded_box(draw, (MARGIN, bullets_top, PAGE_W - MARGIN, bullets_bottom), fill=CARD, outline=LINE, width=3)

        inner_x = MARGIN + 50
        cursor_y = bullets_top + 40
        draw.text((inner_x, cursor_y), "How to use this screen", fill=GREEN, font=small_bold)
        cursor_y += 58

        bullet_max_width = CONTENT_W - 120
        for bullet in page.bullets or []:
            draw.text((inner_x, cursor_y), "•", fill=GREEN, font=body_font)
            cursor_y = draw_wrapped(draw, inner_x + 38, cursor_y, bullet, body_font, TEXT, bullet_max_width - 38, line_gap=12)
            cursor_y += 18

        shot_top = bullets_bottom + 70
        shot_bottom = PAGE_H - 170
        rounded_box(draw, (MARGIN, shot_top, PAGE_W - MARGIN, shot_bottom), fill=CARD_SOFT, outline=LINE, width=3)

        caption = page.caption or ""
        draw.text((MARGIN + 50, shot_top + 36), caption, fill=PURPLE_DARK, font=small_bold)

        if page.screenshot and page.screenshot.exists():
            max_h = shot_bottom - shot_top - 130
            img_shot = fit_image(page.screenshot, max_w=CONTENT_W - 200, max_h=max_h)
            img_x = (PAGE_W - img_shot.width) // 2
            img_y = shot_top + 95 + ((max_h) - img_shot.height) // 2
            img.paste(img_shot, (img_x, img_y))
        else:
            draw.text((MARGIN + 50, shot_top + 120), "Screenshot not available.", fill=MUTED, font=body_font)

    add_page_number(draw, page_num, total)
    return img


def write_markdown():
    lines = [
        "# Kinspace Tablet User Guide",
        "",
        "This guide shows the main tablet screens and the most common things a family member will do:",
        "",
        "- start with Kinspace sign in and tablet onboarding",
        "- view today's tasks",
        "- move to another day",
        "- complete or uncomplete tasks",
        "- add a new to-do",
        "- start and finish a timer",
        "- review or change settings",
        "",
    ]
    for page in PAGES[1:]:
        lines.append(f"## {page.title}")
        lines.append("")
        if page.screenshot:
            lines.append(f"![{page.title}]({page.screenshot.name})")
            lines.append("")
        lines.append(page.subtitle)
        lines.append("")
        for bullet in page.bullets or []:
            lines.append(f"- {bullet}")
        lines.append("")
    OUT_MD.write_text("\n".join(lines), encoding="utf-8")


def main():
    missing = [page.screenshot for page in PAGES if page.screenshot and not page.screenshot.exists()]
    if missing:
        raise FileNotFoundError(f"Missing screenshot(s): {missing}")

    write_markdown()
    pages = []
    for index, page in enumerate(PAGES, start=1):
        if page.kind == "cover":
            pages.append(build_cover(page, index, len(PAGES)))
        else:
            pages.append(build_content(page, index, len(PAGES)))

    if TEMP_PDF.exists():
        TEMP_PDF.unlink()

    with PdfPages(TEMP_PDF) as pdf:
        for page in pages:
            fig = plt.figure(figsize=(8.5, 11), dpi=300)
            ax = fig.add_axes([0, 0, 1, 1])
            ax.imshow(page)
            ax.axis("off")
            pdf.savefig(fig, dpi=300)
            plt.close(fig)

    if OUT_PDF.exists():
        try:
            OUT_PDF.unlink()
        except PermissionError:
            pass
    TEMP_PDF.replace(OUT_PDF)

    print(f"Wrote {OUT_MD}")
    print(f"Wrote {OUT_PDF}")


if __name__ == "__main__":
    main()
