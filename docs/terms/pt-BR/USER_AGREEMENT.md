O HyperAlarm é um módulo não comercial de aprimoramento do sistema, executado no framework Xposed e destinado ao Mi Clock. Ele transforma os alarmes do Mi Clock em gatilhos para scripts root: quando um alarme dispara, um script fornecido por você pode ser executado no lugar do toque, ou em conjunto com ele. Leia e compreenda este Contrato de Usuário do HyperAlarm antes de continuar usando o módulo.

## 0. Versão e data de vigência

- 0.1. Versão do contrato: 1.0
- 0.2. Data de vigência: 15 de novembro de 2026
- 0.3. Este contrato é distribuído com cada compilação; a versão mais recente fica no repositório do projeto.

## 1. Disposições gerais

- 1.1. O HyperAlarm é desenvolvido e mantido pelo Projeto HyperAlarm (doravante, «nós»).
- 1.2. O módulo exige um dispositivo com permissões root e o framework Xposed / LSPosed instalado; se qualquer uma dessas duas condições faltar, ele não funcionará.
- 1.3. O único escopo deste módulo é o Mi Clock (com.android.deskclock). Não injetamos hooks em outros aplicativos.
- 1.4. Ao usar este módulo, você reconhece que ele faz os alarmes executarem um script fornecido por você no horário definido, em vez de apenas tocar.
- 1.5. Você pode parar de usá-lo a qualquer momento desinstalando o módulo ou desativando-o no gerenciador Xposed.

## 2. Escopo da licença

- 2.1. Concedemos a você uma licença pessoal, não transferível, não sublicenciável e não exclusiva para instalar e usar o módulo.
- 2.2. O código-fonte é publicado sob a GNU Affero General Public License v3.0 (AGPL-3.0). Você pode copiá-lo, modificá-lo e redistribuí-lo nos termos dessa licença.
- 2.3. A pré-instalação comercial, a venda em conjunto ou a distribuição com um dispositivo exigem nossa autorização prévia por escrito.
- 2.4. As partes herdadas do HyperCeiler continuam sujeitas à sua licença original; consulte o THIRD_PARTY_NOTICES.md no repositório.
- 2.5. O nome e o ícone do HyperAlarm não estão incluídos na licença do código-fonte.

## 3. Recursos e sua responsabilidade

- 3.1. Os scripts são executados como root no namespace de montagem principal do sistema e, por isso, podem ver os diretórios de dados de outros aplicativos.
- 3.2. Você fornece, instala e mantém os scripts. Não somos responsáveis pelo comportamento dos scripts nem por qualquer consequência de sua execução.
- 3.3. Uma tarefa silenciosa suprime a interface do alarme; se o script estiver ausente, for rejeitado ou o alarme não puder ser reagendado, o módulo mantém o toque normal do alarme.
- 3.4. Este módulo não é um meio confiável de lembrete. Não o use em situações em que um alarme perdido possa causar danos pessoais ou perdas materiais significativas, incluindo, entre outras, situações médicas, de segurança contra incêndio ou de plantão.
- 3.5. Modificar o comportamento do sistema envolve riscos, incluindo, entre outros, o dispositivo não ligar, perda de dados ou perda da garantia.

## 4. Tratamento de informações

- 4.1. Registros de scripts, registros do aplicativo, registros de falhas, configurações e a senha da tela de bloqueio ficam apenas no seu dispositivo. Não os enviamos, coletamos ou vendemos.
- 4.2. O módulo acessa a rede em exatamente dois lugares: a página «Sobre» consulta interfaces públicas do GitHub para ler o avatar do autor e a lista de colaboradores, e o texto do contrato é obtido do GitHub. Ambos os acessos apenas expõem seu endereço IP ao GitHub e não carregam nenhum identificador do dispositivo.
- 4.3. Os detalhes estão na Política de Privacidade do HyperAlarm publicada junto com este contrato.

## 5. Permissões que solicitamos

- 5.1. Root: executar scripts, ler e gravar o diretório de scripts, sincronizar registros e alterar configurações globais. É necessário para os recursos principais.
- 5.2. Lista de aplicativos instalados: para mostrar aplicativos no seletor de escopo.
- 5.3. Notificações: para mostrar a execução de scripts em andamento e os resultados, bem como os motivos de falhas.
- 5.4. Rede: somente nos dois casos descritos no item 4.2.
- 5.5. Você pode recusar qualquer uma delas; o recurso correspondente ficará indisponível, mas os demais não serão afetados.

## 6. Normas de uso

- 6.1. Você não deve remover as informações sobre direitos autorais e licenciamento do módulo ou de suas cópias.
- 6.2. Você não deve usar o módulo para prejudicar a segurança de redes, violar a privacidade de terceiros ou cometer qualquer outro ato contrário à lei.
- 6.3. Você não deve instalar ou usar o módulo em um dispositivo de outra pessoa sem o consentimento do proprietário.
- 6.4. Você não deve usar o módulo para contornar pagamentos, violar a proteção de programas ou burlar controles de acesso.

## 7. Isenção e limitação de responsabilidade

- 7.1. O módulo é fornecido «no estado em que se encontra», sem garantias de qualquer tipo, expressas ou implícitas, incluindo, entre outras, as de comerciabilidade, adequação a uma finalidade específica e não violação de direitos de terceiros.
- 7.2. Verificamos o funcionamento apenas no Mi Clock 18.28.0. Após uma atualização do relógio ou do sistema, alguns recursos podem deixar de funcionar ou mudar de comportamento.
- 7.3. Os problemas causados por suas ações, pelo conteúdo dos seus scripts, pelo seu ambiente root, pelo framework Xposed ou por uma atualização do sistema são de sua responsabilidade.
- 7.4. Na máxima extensão permitida por lei, não somos responsáveis por danos indiretos, incidentais, especiais ou punitivos decorrentes do uso ou da impossibilidade de usar este módulo.
- 7.5. Se alguma cláusula for considerada inválida, as demais permanecerão em vigor.

## 8. Alterações e rescisão

- 8.1. Podemos revisar este contrato por ocasião de uma atualização de versão; o texto revisado é distribuído com a nova compilação e publicado no repositório.
- 8.2. Se você continuar usando o módulo após a entrada em vigor de uma revisão, isso será considerado aceitação; caso não aceite, pare de usar e desinstale o módulo.
- 8.3. Você pode encerrar este contrato a qualquer momento desinstalando o módulo.

## 9. Lei aplicável e resolução de disputas

- 9.1. Este contrato é regido pelas leis da República Popular da China.
- 9.2. As disputas decorrentes deste contrato deverão ser resolvidas primeiro por negociação de boa-fé; não havendo acordo, serão submetidas ao tribunal popular competente de nossa sede principal.

## 10. Contato

- 10.1. Para qualquer questão relacionada a este contrato, envie um relato pelo repositório do projeto.
- 10.2. Se você acredita que alguma cláusula não corresponde ao comportamento real, pedimos que nos avise para corrigi-la, em vez de considerá-la vinculante.
