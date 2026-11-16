import {beforeEach, describe, expect, it, vi} from 'vitest';
import {fetchConvocation, filenameFrom, openConvocation, saveConvocation} from './convocation';

describe('convocation PDF', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('extrait les noms filename et filename* sans accepter de chemin', () => {
    expect(filenameFrom('inline; filename="convocation-42.pdf"')).toBe('convocation-42.pdf');
    expect(filenameFrom("attachment; filename*=UTF-8''convocation-rdv-%C3%A9t%C3%A9-42.pdf")).toBe('convocation-rdv-été-42.pdf');
    expect(filenameFrom('attachment; filename="../private/report"')).toBe('report.pdf');
    expect(filenameFrom(null, 'convocation-rdv-42')).toBe('convocation-rdv-42.pdf');
  });

  it('ne transforme pas une réponse JSON en blob PDF', async () => {
    const request = vi.fn().mockResolvedValue(new Response('{"detail":"Erreur"}', {
      status: 200,
      headers: {'Content-Type': 'application/json'},
    }));

    await expect(fetchConvocation(42, request)).rejects.toThrow('pas renvoyé un document PDF');
  });

  it.each([
    [401, 'Connectez-vous'],
    [404, 'introuvable'],
    [409, 'annulé'],
    [500, 'Réessayez plus tard'],
  ])('traduit le code HTTP %i en message clair', async (status, message) => {
    const request = vi.fn().mockResolvedValue(new Response('{"detail":"interne"}', {status}));
    await expect(fetchConvocation(42, request)).rejects.toThrow(message);
  });

  it('ouvre un onglet avant de commencer la requête', async () => {
    const order: string[] = [];
    const popup = {location: {href: ''}, close: vi.fn()};
    const open = vi.fn(() => {
      order.push('popup');
      return popup;
    });
    const createObjectURL = vi.fn(() => 'blob:convocation');
    const setTimeout = vi.fn((_callback: () => void, _delay: number) => 0);
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('window', {open, setTimeout});
    vi.stubGlobal('URL', {createObjectURL, revokeObjectURL});
    const request = vi.fn(async () => {
      order.push('fetch');
      return new Response('%PDF-test', {
        status: 200,
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'inline; filename="convocation-42.pdf"',
        },
      });
    });

    await openConvocation(42, request);

    expect(order).toEqual(['popup', 'fetch']);
    expect(popup.location.href).toBe('blob:convocation');
    expect(setTimeout.mock.calls[0][1]).toBe(60_000);
    setTimeout.mock.calls[0][0]();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:convocation');
  });

  it('déclenche le téléchargement sous le nom reçu du serveur', async () => {
    const anchor = {href: '', download: '', style: {}, click: vi.fn(), remove: vi.fn()};
    const append = vi.fn();
    vi.stubGlobal('document', {createElement: vi.fn(() => anchor), body: {append}});
    const setTimeout = vi.fn((_callback: () => void, _delay: number) => 0);
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('window', {setTimeout});
    vi.stubGlobal('URL', {createObjectURL: vi.fn(() => 'blob:convocation'), revokeObjectURL});
    const request = vi.fn().mockResolvedValue(new Response('%PDF-test', {
      status: 200,
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': 'attachment; filename="convocation-42.pdf"',
      },
    }));

    await saveConvocation(42, request);

    expect(anchor.download).toBe('convocation-42.pdf');
    expect(anchor.click).toHaveBeenCalledOnce();
    expect(anchor.remove).toHaveBeenCalledOnce();
    expect(setTimeout.mock.calls[0][1]).toBe(1_000);
    setTimeout.mock.calls[0][0]();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:convocation');
  });
});
