# Topic
客户端无需主动订阅任何主题，服务器自动订阅。

客户端发出的主题有明确规则，请遵循规则否则无法发出：

REQUEST/{userName}/{sceneName}/{serverName}

上面有3个参数其中userName是指用户自己的用户名，sceneName指哪个游戏场景，serverName指需要放给哪个服务器

其中userName传错误则无法发出，serverName是在创建房间后指定处理服务器时用到的参数，还未创建房间前不知道是哪个服务器用all代替

# 创建房间
Topic： REQUEST/client2/demoB/all

{ "requestId": "123","type":"CreateRoomMsg" }

# 离开房间
Topic： REQUEST/client2/demoB/all

{ "requestId": "123","type":"LeaveRoomMsg","body": {"roomName":"1583811395562"} }

#匹配
Topic： REQUEST/client2/demoB/all

{ "requestId": "123","type":"MatchMsg","body": {"roomLevel":"high"} }

#取消匹配
Topic： REQUEST/client2/demoB/all

{ "requestId": "123","type":"CancelMatchMsg","body": {"roomLevel":"high"} }