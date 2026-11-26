<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | **Português (Brasil)** | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm é um módulo Xposed cujo **único escopo é o Relógio da Xiaomi**. Ele transforma os alarmes do relógio em gatilhos para scripts de shell.

## App hospedeiro

Só as versões abaixo foram verificadas. Nas demais, teste você mesmo, envie um APK para adaptação ou atualize/volte para estas:

- Relógio da Xiaomi `com.android.deskclock`
- Versão `18.28.0`, código de versão `1306002800`

## Instalação

1. Tenha root (KernelSU / Magisk) e LSPosed (Xposed API 101 ou superior).
2. Instale o módulo e ative o HyperAlarm no LSPosed.
3. Marque como escopo apenas `com.android.deskclock`. Não adicione outros pacotes.
4. Force a parada do Relógio e deixe o processo subir de novo, senão o módulo não faz nada.

## Uso

- Nova tarefa: o `+` à direita da barra de ações superior.
- O rótulo do alarme é o nome do script e precisa terminar em `.sh`:
  - `alert.sh`: executa em silêncio na hora marcada, sem toque e sem tela de alarme
  - `@alert.sh`: executa o script e mantém a tela do alarme e o toque
- O `@` fica oculto na lista; o botão «Toque» alterna entre os dois modos.
- «Executar agora» testa um script sem esperar a hora.
- Tarefas de turno disparam por turno, calculado a partir da data de início e do ciclo.

## Regras dos scripts

Os scripts ficam em `/data/adb/alarm_shell/scripts/` e o nome no rótulo precisa bater com o do arquivo.

```text
/data/adb/alarm_shell/scripts/      # pasta de scripts
/data/adb/alarm_shell/password      # senha da tela de bloqueio
/data/adb/alarm_shell/unlock_phone  # script de desbloqueio padrão
```

- As quebras de linha precisam ser LF. Um arquivo com CRLF do Windows simplesmente falha.
- Os scripts rodam como root no namespace principal do sistema, então enxergam as pastas de dados de outros apps.
- Cada execução deixa log: aparece em «Logs de scripts» no app e também é espelhado em
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<nome do script>/`.
- Antes de uma tarefa silenciosa, o módulo só acorda e desbloqueia a tela quando o dispositivo ainda está bloqueado (por exemplo, antes do primeiro desbloqueio após reiniciar). Uma tela apenas apagada continua apagada.

## Configurações

- **Relógio mostra só alarmes**: a lista do Relógio mantém os alarmes reais e esconde as tarefas de script.
- **Ocultar tarefas com toque**: esconde também as tarefas com toque ativado.
- Nível de log / Logs do app / Logs de scripts: suba o nível antes de investigar um problema.
- **Senha da tela de bloqueio**: desbloqueia tarefas silenciosas apenas enquanto o dispositivo ainda estiver bloqueado após reiniciar; vazio significa não alterar.
- Tutorial de reinício, copiar a pasta de scripts, backup e restauração da configuração.

## Limitações conhecidas

- Verificado só em Android 16 / HyperOS 3 / Relógio 18.28.0; outras combinações são por sua conta.
- O modo por turnos depende das entranhas do Relógio e precisa ser verificado de novo após atualizar o relógio.
- Sem root o módulo não funciona; a página de configurações avisa.
- Os logs de scripts são espelhados para o armazenamento externo e podem ser lidos por root e por adb shell. Não imprima segredos no log.

## Perguntas frequentes

- **Criei uma tarefa e o relógio não reage**: confira se o módulo está ativo no LSPosed, se o escopo é só o Relógio e se o processo do relógio foi reiniciado.
- **A tarefa não aparece na lista do relógio**: é o «Relógio mostra só alarmes» fazendo o trabalho dele; desligue.
- **Chegou a hora e nada rodou**: veja os logs de scripts no app; script ausente ou rótulo errado são avisados ali.

## Licença

- Código próprio deste repositório: **AGPL-3.0** (veja `../LICENSE`)
- Componentes de terceiros e suas licenças: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Agradecimentos

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): o ponto de partida do projeto; boa parte da interface MIUIX e das configurações vem de lá
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): componentes de interface no estilo Xiaomi
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): APIs de root e Xposed
- Todo mundo que abriu issues e mandou logs
